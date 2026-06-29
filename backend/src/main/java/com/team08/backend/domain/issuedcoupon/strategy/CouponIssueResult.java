package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;

public record CouponIssueResult(
        Status status,
        Long userId,
        Long policyId,
        IssuedCoupon issuedCoupon
) {
    public enum Status {
        ISSUED, REQUESTED
    }

    public static CouponIssueResult issued(IssuedCoupon issuedCoupon) {
        return new CouponIssueResult(Status.ISSUED, issuedCoupon.getUserId(), issuedCoupon.getPolicyId(), issuedCoupon);
    }

    public static CouponIssueResult requested(Long userId, Long policyId) {
        return new CouponIssueResult(Status.REQUESTED, userId, policyId, null);
    }
}
