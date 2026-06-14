package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;
    private final IssuedCouponStrategyFactory strategyFactory;

    // TODO 나중에 회원가입에 추가
    // [시스템] 가입 기념 쿠폰 자동 발급
    @Transactional
    public void issueSignUpCoupon(Long userId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 타입으로 정책 단건 조회 (주로 자동 발급용 AUTO 타입 조회 시 사용)
        CouponPolicy policy = couponPolicyRepository.findByCouponType(CouponType.AUTO)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 쿠폰 발급 기록
        IssuedCoupon newCoupon = IssuedCoupon.issue(
                policy.getId(),
                userId,
                policy.calculateExpirationDate()
        );

        // 쿠폰 발급 저장 및 동시성 방어
        try {
            issuedCouponRepository.saveAndFlush(newCoupon);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    // [시스템] 출석 보상 쿠폰 자동 발급
    @Transactional
    public void issueAttendanceCoupon(Long userId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 이름으로 정책 단건 조회 (고정된 이름 정책 사용)
        CouponPolicy policy = couponPolicyRepository.findByName("연속 출석 보상 쿠폰")
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 쿠폰 발급 기록 생성
        IssuedCoupon newCoupon = IssuedCoupon.issue(
                policy.getId(),
                userId,
                policy.calculateExpirationDate()
        );

        // 쿠폰 발급 저장 및 동시성 방어
        try {
            issuedCouponRepository.saveAndFlush(newCoupon);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    // [사용자] 쿠폰 다운로드
    @Transactional
    public IssuedCouponResponse downloadCoupon(Long userId, Long policyId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 타입에 맞는 전략 선택
        IssuedCouponStrategy strategy = strategyFactory.getStrategy(policy.getCouponType());

        // 쿠폰 발급 로직 실행 및 저장
        IssuedCoupon savedCoupon = strategy.issue(userId, policyId);
        return IssuedCouponResponse.from(savedCoupon);
    }

    // [사용자] 내 쿠폰 목록 조회
    @Transactional(readOnly = true)
    public List<CouponListResponse> getMyCoupons(Long userId) {
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findByUserIdOrderByExpiredAtAsc(userId);

        // 쿠폰 정책 ID 목록 추출
        List<Long> policyIds = issuedCoupons.stream()
                .map(IssuedCoupon::getPolicyId)
                .distinct()
                .toList();

        // 정책 정보 한꺼번에 조회 및 맵으로 변환
        Map<Long, CouponPolicy> policyMap = couponPolicyRepository.findAllById(policyIds).stream()
                .collect(Collectors.toMap(CouponPolicy::getId, policy -> policy));

        return issuedCoupons.stream()
                .map(coupon -> {
                    CouponPolicy policy = policyMap.get(coupon.getPolicyId());
                    if (policy == null) {
                        throw new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND);
                    }
                    return CouponListResponse.of(coupon, policy);
                })
                .toList();
    }

    // [사용자] 쿠폰 적용 시 예상 할인 금액 조회 (결제 전 화면 용 API)
    @Transactional(readOnly = true)
    public ExpectedDiscountResponse calculateExpectedDiscount(Long userId, Long issuedCouponId, int originalPrice) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // 본인 쿠폰인지 검증
        if (!issuedCoupon.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.COUPON_NOT_OWNED);
        }

        // 상태/만료일 검증
        if (issuedCoupon.getStatus() != CouponStatus.ISSUED) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED_OR_EXPIRED);
        }

        CouponPolicy policy = couponPolicyRepository.findById(issuedCoupon.getPolicyId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 할인 예상 금액 계산
        int discountAmount = policy.calculateDiscountAmount(originalPrice);

        // 할인된 최종 가격 계산 (0원 이하 방어)
        int finalPrice = Math.max(0, originalPrice - discountAmount);

        // 결과 DTO 반환
        return new ExpectedDiscountResponse(
                policy.getName(),
                originalPrice,
                discountAmount,
                finalPrice
        );
    }

    // TODO 나중에 결제에 추가
    // [시스템] 결제 시 쿠폰 사용 처리
    @Transactional
    public int useCouponForOrder(Long userId, Long issuedCouponId, int originalPrice) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        // 본인 쿠폰인지 검증
        if (!issuedCoupon.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.COUPON_NOT_OWNED);
        }

        // 상태/만료일 검증 (사용 전 다시 한번 체크)
        if (issuedCoupon.getStatus() != CouponStatus.ISSUED) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED_OR_EXPIRED);
        }

        CouponPolicy policy = couponPolicyRepository.findById(issuedCoupon.getPolicyId())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        // 할인 금액 계산
        int discountAmount = policy.calculateDiscountAmount(originalPrice);

        // 쿠폰 사용 처리
        if (policy.getUsageType() == CouponUsageType.SINGLE_USE) {
            issuedCoupon.use();
        } else {
            issuedCoupon.recordUsage();
        }

        // 최종 할인된 금액 반환
        return discountAmount;
    }
}
