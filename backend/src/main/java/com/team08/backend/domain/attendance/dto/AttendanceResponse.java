package com.team08.backend.domain.attendance.dto;

import com.team08.backend.domain.attendance.entity.Attendance;
import lombok.Builder;

@Builder
public record AttendanceResponse(
        int consecutiveDays,
        int monthlyTotalDays
) {
    public static AttendanceResponse from(Attendance log) {
        return AttendanceResponse.builder()
                .consecutiveDays(log.getConsecutiveDays())
                .monthlyTotalDays(log.getMonthlyTotalDays())
                .build();
    }
}
