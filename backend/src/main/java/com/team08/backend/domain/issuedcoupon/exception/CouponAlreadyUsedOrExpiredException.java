package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponAlreadyUsedOrExpiredException extends IssuedCouponException {
    public CouponAlreadyUsedOrExpiredException() {
        super(ErrorCode.COUPON_ALREADY_USED_OR_EXPIRED);
    }
}
