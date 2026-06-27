package com.team08.backend.domain.attendance.event;

import java.time.LocalDate;

public record AttendanceCheckedEvent(
        Long userId,
        LocalDate attendanceDate,
        int consecutiveDays,
        int monthlyTotalDays
) {
}
