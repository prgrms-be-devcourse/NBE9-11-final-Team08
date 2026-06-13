package com.team08.backend.domain.attendance.dto;

import com.team08.backend.domain.attendance.entity.Attendance;

public record AttendanceResponse(
        int consecutiveDays,
        int monthlyTotalDays
) {
    public static AttendanceResponse from(Attendance log) {
        return new AttendanceResponse(
                log.getConsecutiveDays(),
                log.getMonthlyTotalDays()
        );
    }
}
