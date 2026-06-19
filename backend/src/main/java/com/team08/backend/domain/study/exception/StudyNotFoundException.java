package com.team08.backend.domain.study.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class StudyNotFoundException extends CustomException {
    public StudyNotFoundException() {
        super(ErrorCode.STUDY_NOT_FOUND);
    }
}
