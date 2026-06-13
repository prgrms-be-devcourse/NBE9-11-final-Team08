package com.team08.backend.domain.auth.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class InvalidSignupRoleException extends CustomException {
    public InvalidSignupRoleException() {
        super(ErrorCode.INVALID_SIGNUP_ROLE);
    }
}
