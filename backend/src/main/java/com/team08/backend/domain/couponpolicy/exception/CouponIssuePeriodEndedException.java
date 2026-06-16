package com.team08.backend.domain.couponpolicy.exception;

import com.team08.backend.global.exception.ErrorCode;

public class CouponIssuePeriodEndedException extends CouponPolicyException {
    public CouponIssuePeriodEndedException() {
        super(ErrorCode.COUPON_ISSUE_PERIOD_ENDED);
    }
}
