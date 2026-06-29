package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;

import java.time.Clock;
import java.time.LocalDateTime;

public abstract class AbstractIssuedCouponStrategy implements IssuedCouponStrategy {

    private final Clock clock;

    protected AbstractIssuedCouponStrategy(Clock clock) {
        this.clock = clock;
    }

    // 쿠폰 발급 공통 로직
    @Override
    public CouponIssueResult issue(Long userId, Long policyId) {
        CouponPolicy policy = findPolicy(policyId);
        LocalDateTime now = LocalDateTime.now(clock);

        // 타입별 중복 발급 체크
        validateDuplicateIssue(userId, policyId);

        // 쿠폰 발급 기간 검증
        policy.validateIssuePeriod(now);

        // 쿠폰 발급 전략별 핵심 발급 및 응답 생성 (Template Method)
        return processIssue(userId, policy, now);
    }

    // 쿠폰 정책 조회
    protected abstract CouponPolicy findPolicy(Long policyId);

    // 타입별 중복 발급 체크
    protected abstract void validateDuplicateIssue(Long userId, Long policyId);

    // 타입별 핵심 발급 처리
    protected abstract CouponIssueResult processIssue(Long userId, CouponPolicy policy, LocalDateTime now);
}
