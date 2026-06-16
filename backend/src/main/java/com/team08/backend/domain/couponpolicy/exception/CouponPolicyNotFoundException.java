package com.team08.backend.domain.couponpolicy.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponPolicyNotFoundException extends CouponPolicyException {
    public CouponPolicyNotFoundException() {
        super(ErrorCode.COUPON_POLICY_NOT_FOUND);
    }
}
