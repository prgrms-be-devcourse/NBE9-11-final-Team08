package com.team08.backend.domain.couponreward.outbox.dto;

import java.time.LocalDate;

public record AttendanceRewardPayload(
        Long userId,
        LocalDate attendanceDate,
        int consecutiveDays,
        int monthlyTotalDays
) {
}
