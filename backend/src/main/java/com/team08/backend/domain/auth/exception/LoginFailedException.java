package com.team08.backend.domain.auth.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class LoginFailedException extends CustomException {
    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
