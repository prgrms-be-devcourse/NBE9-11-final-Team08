package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponNotOwnedException extends IssuedCouponException {
    public CouponNotOwnedException() {
        super(ErrorCode.COUPON_NOT_OWNED);
    }
}
