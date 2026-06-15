package com.team08.backend.domain.study.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class DuplicateStudyException extends CustomException {
    public DuplicateStudyException() {
        super(ErrorCode.DUPLICATE_STUDY);
    }
}
