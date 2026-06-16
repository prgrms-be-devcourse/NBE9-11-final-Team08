package com.team08.backend.domain.attendance.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public class AttendanceException extends CustomException {
    public AttendanceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
