package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyIssuedException;
import com.team08.backend.domain.issuedcoupon.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.issuedcoupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FcfsIssuedCouponStrategy implements IssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    @Override
    public CouponType getSupportedType() {
        return CouponType.FCFS;
    }

    // [사용자] 선착순 쿠폰 발급 로직
    @Override
    @Transactional
    public IssuedCoupon issue(Long userId, Long policyId) {
        // 비관적 락을 적용한 쿠폰 정책 조회
        CouponPolicy policy = couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);

        LocalDateTime now = LocalDateTime.now(clock);

        // 중복 발급 체크
        if (issuedCouponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new CouponAlreadyIssuedException();
        }

        // 쿠폰 발급 기간 검증
        policy.validateIssuePeriod(); // TODO: Policy 쪽도 Clock 적용 시 파라미터로 now 전달 필요

        // 쿠폰 수량 차감 및 재고 소진 체크
        policy.decreaseQuantity();

        // 쿠폰 발급 기록 생성
        IssuedCoupon newCoupon = IssuedCoupon.create(policy, userId, now);

        // 동시성 방어
        return issuedCouponRepository.saveWithConcurrencyProtection(newCoupon);
    }
}
