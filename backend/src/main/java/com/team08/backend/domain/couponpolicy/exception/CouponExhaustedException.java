package com.team08.backend.domain.couponpolicy.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponExhaustedException extends CouponPolicyException {
    public CouponExhaustedException() {
        super(ErrorCode.COUPON_EXHAUSTED);
    }
}
