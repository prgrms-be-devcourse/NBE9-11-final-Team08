package com.team08.backend.domain.issuedcoupon.service;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponListResponse;
import com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse;
import com.team08.backend.domain.issuedcoupon.dto.IssuedCouponResponse;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponNotFoundException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategy;
import com.team08.backend.domain.issuedcoupon.strategy.IssuedCouponStrategyFactory;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.service.IssuedCouponJobWriter;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private static final String ATTENDANCE_REWARD_COUPON_NAME = "연속 출석 보상 쿠폰";

    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserRepository userRepository;
    private final IssuedCouponStrategyFactory strategyFactory;
    private final IssuedCouponWriter issuedCouponWriter;
    private final IssuedCouponJobWriter issuedCouponJobWriter;
    private final Clock clock;

    // TODO 나중에 회원가입에 추가
    // [시스템] 가입 기념 쿠폰 자동 발급
    @Transactional
    public void issueSignUpCoupon(Long userId) {
        // 쿠폰 타입으로 정책 조회 및 발급
        CouponPolicy policy = couponPolicyRepository.findByCouponType(CouponType.AUTO)
                .orElseThrow(CouponPolicyNotFoundException::new);

        issueSystemCoupon(userId, policy);
    }

    // [시스템] 출석 보상 쿠폰 자동 발급
    @Transactional
    public void issueAttendanceCoupon(Long userId) {
        // 쿠폰 이름으로 정책 조회 및 발급
        CouponPolicy policy = couponPolicyRepository.findByName(ATTENDANCE_REWARD_COUPON_NAME)
                .orElseThrow(CouponPolicyNotFoundException::new);

        issueSystemCoupon(userId, policy);
    }

    // 시스템 공통 쿠폰 발급 처리
    private void issueSystemCoupon(Long userId, CouponPolicy policy) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 발급 기록 생성
        IssuedCoupon newCoupon = IssuedCoupon.create(policy, userId, LocalDateTime.now(clock));

        // 쿠폰 발급 저장 및 동시성 방어
        issuedCouponWriter.saveWithConcurrencyProtection(newCoupon);
    }

    // [사용자] 쿠폰 다운로드
    @Transactional
    public IssuedCouponResponse downloadCoupon(Long userId, Long policyId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 쿠폰 타입 조회
        CouponType couponType = couponPolicyRepository.findCouponTypeById(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        // 타입에 맞는 전략 선택
        IssuedCouponStrategy strategy = strategyFactory.getStrategy(couponType);

        // 쿠폰 발급 로직 실행 및 저장
        IssuedCoupon newCoupon = strategy.issue(userId, policyId);
        IssuedCouponJob issuedCouponJob = issuedCouponJobWriter.createRequested(
                userId,
                policyId,
                LocalDateTime.now(clock)
        );
        try {
            IssuedCoupon savedCoupon = issuedCouponWriter.saveWithConcurrencyProtection(newCoupon);
            issuedCouponJobWriter.markIssued(issuedCouponJob.getId(), LocalDateTime.now(clock));
            return IssuedCouponResponse.from(savedCoupon);
        } catch (RuntimeException e) {
            issuedCouponJobWriter.markFailed(issuedCouponJob.getId(), e.getClass().getSimpleName(), LocalDateTime.now(clock));
            // 쿠폰 발급 실패 보상
            strategy.rollbackIssue(userId, policyId);
            throw e;
        }
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

        LocalDateTime now = LocalDateTime.now(clock);
        return issuedCoupons.stream()
                .map(coupon -> {
                    CouponPolicy policy = policyMap.get(coupon.getPolicyId());
                    if (policy == null) {
                        throw new CouponPolicyNotFoundException();
                    }
                    return CouponListResponse.of(coupon, policy, now);
                })
                .toList();
    }

    // [사용자] 쿠폰 적용 시 예상 할인 금액 조회 (결제 전 화면 용 API)
    @Transactional(readOnly = true)
    public ExpectedDiscountResponse calculateExpectedDiscount(Long userId, Long issuedCouponId, int originalPrice) {
        CouponUsageContext context = getUsableCouponContext(userId, issuedCouponId, LocalDateTime.now(clock));
        CouponPolicy policy = context.couponPolicy();

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
        LocalDateTime now = LocalDateTime.now(clock);
        CouponUsageContext context = getUsableCouponContext(userId, issuedCouponId, now);
        CouponPolicy policy = context.couponPolicy();

        // 할인 금액 계산
        int discountAmount = policy.calculateDiscountAmount(originalPrice);

        // 쿠폰 사용 처리
        context.issuedCoupon().applyUsage(policy.getUsageType(), now);

        // 최종 할인된 금액 반환
        return discountAmount;
    }

    // 사용 가능한 쿠폰과 정책 조회
    private CouponUsageContext getUsableCouponContext(Long userId, Long issuedCouponId, LocalDateTime now) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(CouponNotFoundException::new);

        // 사용 가능 여부 검증
        issuedCoupon.validateUsable(userId, now);

        CouponPolicy policy = couponPolicyRepository.findById(issuedCoupon.getPolicyId())
                .orElseThrow(CouponPolicyNotFoundException::new);

        return new CouponUsageContext(issuedCoupon, policy);
    }

    private record CouponUsageContext(
            IssuedCoupon issuedCoupon,
            CouponPolicy couponPolicy
    ) {
    }
}
