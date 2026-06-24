package com.team08.backend.domain.couponpolicy.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponIssuePeriodNotStartedException extends CouponPolicyException {
    public CouponIssuePeriodNotStartedException() {
        super(ErrorCode.COUPON_ISSUE_PERIOD_NOT_STARTED);
    }
}
