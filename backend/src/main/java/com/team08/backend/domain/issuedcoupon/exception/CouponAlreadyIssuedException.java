package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponAlreadyIssuedException extends IssuedCouponException {
    public CouponAlreadyIssuedException() {
        super(ErrorCode.COUPON_ALREADY_ISSUED);
    }
}
