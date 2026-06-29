package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.ErrorCode;

public class JobAlreadyProcessingException extends IssuedCouponException {
    public JobAlreadyProcessingException() {
        super(ErrorCode.JOB_ALREADY_PROCESSING);
    }
}
