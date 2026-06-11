package com.team08.backend.domain.study.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class InvalidStudyStatusTransitionException extends CustomException {
    public InvalidStudyStatusTransitionException() {
        super(ErrorCode.INVALID_STUDY_STATUS_TRANSITION);
    }
}
