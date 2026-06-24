package com.team08.backend.domain.studyreport.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class StudyReportNotFoundException extends CustomException {
    public StudyReportNotFoundException() {
        super(ErrorCode.NOT_STUDY_MEMBER);
    }
}
