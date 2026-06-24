package com.team08.backend.domain.couponpolicy.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class CouponPolicyException extends CustomException {
    public CouponPolicyException(ErrorCode errorCode) {
        super(errorCode);
    }
}
