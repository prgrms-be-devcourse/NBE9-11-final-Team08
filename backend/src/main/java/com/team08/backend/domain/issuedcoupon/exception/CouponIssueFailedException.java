package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponIssueFailedException extends IssuedCouponException {
    public CouponIssueFailedException() {
        super(ErrorCode.COUPON_ISSUE_FAILED);
    }
}
