package com.team08.backend.domain.attendance.exception;

import com.team08.backend.global.exception.ErrorCode;

public class AttendanceAlreadyExistsException extends AttendanceException {
    public AttendanceAlreadyExistsException() {
        super(ErrorCode.ATTENDANCE_ALREADY_EXISTS);
    }
}
