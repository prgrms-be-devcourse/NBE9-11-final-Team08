package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class InvalidCouponTypeException extends IssuedCouponException {
    public InvalidCouponTypeException() {
        super(ErrorCode.INVALID_COUPON_TYPE);
    }
}
