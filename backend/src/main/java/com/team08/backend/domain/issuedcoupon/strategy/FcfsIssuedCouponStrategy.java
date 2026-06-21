package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.exception.CouponPolicyNotFoundException;
import com.team08.backend.domain.couponpolicy.repository.CouponPolicyRepository;
import com.team08.backend.domain.issuedcoupon.service.FcfsCouponRedisIssuer;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class FcfsIssuedCouponStrategy extends AbstractIssuedCouponStrategy {

    private final CouponPolicyRepository couponPolicyRepository;
    private final FcfsCouponRedisIssuer fcfsCouponRedisIssuer;

    public FcfsIssuedCouponStrategy(
            Clock clock,
            CouponPolicyRepository couponPolicyRepository,
            FcfsCouponRedisIssuer fcfsCouponRedisIssuer
    ) {
        super(clock);
        this.couponPolicyRepository = couponPolicyRepository;
        this.fcfsCouponRedisIssuer = fcfsCouponRedisIssuer;
    }

    @Override
    public CouponType getSupportedType() {
        return CouponType.FCFS;
    }

    // 선착순 쿠폰 정책 조회
    @Override
    protected CouponPolicy findPolicy(Long policyId) {
        // 비관적 락을 적용한 쿠폰 정책 조회
        return couponPolicyRepository.findByIdWithLock(policyId)
                .orElseThrow(CouponPolicyNotFoundException::new);
    }

    // 선착순 쿠폰 중복 발급 체크
    @Override
    protected void validateDuplicateIssue(Long userId, Long policyId) {
    }

    // 선착순 쿠폰 발급 전 처리
    @Override
    protected void beforeIssue(Long userId, Long policyId, CouponPolicy policy) {
        // Redis 선착순 발급 확정
        fcfsCouponRedisIssuer.issue(userId, policy);
    }

}
