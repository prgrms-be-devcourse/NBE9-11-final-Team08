package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponPolicyNotFoundException extends IssuedCouponException {
    public CouponPolicyNotFoundException() {
        super(ErrorCode.COUPON_POLICY_NOT_FOUND);
    }
}
