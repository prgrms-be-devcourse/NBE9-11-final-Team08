package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponType;

public interface IssuedCouponStrategy {
    
    CouponType getSupportedType();

    CouponIssueResult issue(Long userId, Long policyId);
}
