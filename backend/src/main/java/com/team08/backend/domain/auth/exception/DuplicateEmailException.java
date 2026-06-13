package com.team08.backend.domain.auth.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class DuplicateEmailException extends CustomException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
