package com.team08.backend.domain.coupon.service;

import com.team08.backend.domain.coupon.dto.CouponListResponse;
import com.team08.backend.domain.coupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.coupon.entity.CouponPolicy;
import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;
import com.team08.backend.domain.coupon.repository.CouponPolicyRepository;
import com.team08.backend.domain.coupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;

    // TODO 나중에 유저 생성 로직에 추가
    @Transactional
    public void issueSignUpCoupon(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없음"));
        // 쿠폰 타입으로 정책 단건 조회 (주로 자동 발급용 AUTO 타입 조회 시 사용)
        CouponPolicy policy = couponPolicyRepository.findByCouponType(CouponType.AUTO)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰 정책을 찾을 수 없음"));

        // [시스템] 쿠폰 발급 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(policy.calculateExpirationDate())
                .build();

        issuedCouponRepository.save(newCoupon);
    }

    // [시스템] 출석 보상 쿠폰 자동 발급
    @Transactional
    public void issueAttendanceCoupon(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없음"));

        // 쿠폰 이름으로 정책 단건 조회
        CouponPolicy policy = couponPolicyRepository.findByName("연속 출석 보상 쿠폰")
                .orElseThrow(() -> new IllegalArgumentException("출석 쿠폰 정책을 찾을 수 없음"));

        // 쿠폰 발급 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(policy.calculateExpirationDate())
                .build();

        issuedCouponRepository.save(newCoupon);
    }

    // [사용자] 일반 쿠폰 다운로드
    @Transactional
    public void downloadCoupon(Long userId, Long policyId) {
        // 중복 발급 검증
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없음"));

        // 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 일반 다운로드 쿠폰 여부 검증
        if (policy.getCouponType() != CouponType.NORMAL) {
            throw new IllegalArgumentException("일반 다운로드 전용 쿠폰이 아닙니다.");
        }

        // 쿠폰 발급 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(policy.calculateExpirationDate())
                .build();

        issuedCouponRepository.save(newCoupon);
    }
    
    // TODO 락 범위 조절
    // [사용자] 선착순 쿠폰 다운로드
    @Transactional
    public void downloadFcfsCoupon(Long userId, Long policyId) {
        // 중복 발급 검증
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없음"));

        // 비관적 락을 적용한 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 선착순 쿠폰 여부 검증
        if (policy.getCouponType() != CouponType.FCFS) {
            throw new IllegalArgumentException("선착순 발급 전용 쿠폰이 아닙니다.");
        }

        // 쿠폰 발급 기간 검증
        LocalDateTime now = LocalDateTime.now();
        if (policy.getIssueStartDate() != null && now.isBefore(policy.getIssueStartDate())) {
            throw new IllegalStateException("아직 쿠폰 발급 기간이 시작되지 않았습니다.");
        }
        if (policy.getIssueEndDate() != null && now.isAfter(policy.getIssueEndDate())) {
            throw new IllegalStateException("쿠폰 발급 기간이 종료되었습니다.");
        }

        // 쿠폰 수량 차감
        policy.decreaseQuantity();

        // 쿠폰 발급 및 저장
        IssuedCoupon newCoupon = IssuedCoupon.builder()
                .user(user)
                .policy(policy)
                .expiredAt(policy.calculateExpirationDate())
                .build();

        issuedCouponRepository.save(newCoupon);
    }

    // [사용자] 내 쿠폰 목록 조회 (나중에 수정 필요할 수도)
    @Transactional(readOnly = true)
    public List<CouponListResponse> getMyCoupons(Long userId) {
        return issuedCouponRepository.findByUserIdOrderByIssuedAtDesc(userId)
                .stream()
                .map(CouponListResponse::from)
                .toList();
    }

    // [사용자] 쿠폰 적용 시 예상 할인 금액 조회 (결제 전 화면 용 API)
    @Transactional(readOnly = true)
    public ExpectedDiscountResponse calculateExpectedDiscount(Long userId, Long issuedCouponId, int originalPrice) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 본인 쿠폰인지 검증
        if (!issuedCoupon.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
        }

        // 상태/만료일 검증
        if (issuedCoupon.getStatus() != CouponStatus.ISSUED) {
            throw new IllegalStateException("사용할 수 없는 쿠폰 상태입니다.");
        }

        // 할인 예상 금액 계산
        int discountAmount = issuedCoupon.getPolicy().calculateDiscountAmount(originalPrice);

        // 할인된 최종 가격 계산 (0원 이하 방어)
        int finalPrice = Math.max(0, originalPrice - discountAmount);

        // 결과 DTO 반환
        return new ExpectedDiscountResponse(
                issuedCoupon.getPolicy().getName(),
                originalPrice,
                discountAmount,
                finalPrice
        );
    }

    // TODO 나중에 결제 로직에 추가
    // [시스템] 결제 시 쿠폰 사용 처리 
    @Transactional
    public int useCouponForOrder(Long userId, Long issuedCouponId, int originalPrice) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 본인 쿠폰인지 검증
        if (!issuedCoupon.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
        }

        // 할인 금액 계산
        int discountAmount = issuedCoupon.getPolicy().calculateDiscountAmount(originalPrice);

        // [시스템] 쿠폰 사용 처리 (ISSUED -> USED)
        issuedCoupon.use();

        // 최종 할인된 금액 반환 
        return discountAmount;
    }
}
