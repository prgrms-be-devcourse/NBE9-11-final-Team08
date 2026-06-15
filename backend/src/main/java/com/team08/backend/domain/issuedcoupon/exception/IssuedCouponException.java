package com.team08.backend.domain.issuedcoupon.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class IssuedCouponException extends CustomException {
    public IssuedCouponException(ErrorCode errorCode) {
        super(errorCode);
    }
}
