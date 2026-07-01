package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;

import java.time.Clock;
import java.time.LocalDateTime;

public abstract class AbstractIssuedCouponStrategy implements IssuedCouponStrategy {

    private final Clock clock;

    protected AbstractIssuedCouponStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CouponIssueResult issue(Long userId, Long policyId) {
        CouponPolicy policy = findPolicy(policyId);
        LocalDateTime now = LocalDateTime.now(clock);

        validateDuplicateIssue(userId, policyId);

        policy.validateIssuePeriod(now);

        return processIssue(userId, policy, now);
    }

    protected abstract CouponPolicy findPolicy(Long policyId);

    protected abstract void validateDuplicateIssue(Long userId, Long policyId);

    protected abstract CouponIssueResult processIssue(Long userId, CouponPolicy policy, LocalDateTime now);
}
