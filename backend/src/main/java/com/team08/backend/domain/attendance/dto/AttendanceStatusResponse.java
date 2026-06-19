package com.team08.backend.domain.attendance.dto;

import java.util.List;

public record AttendanceStatusResponse(
        boolean checkedToday,
        int consecutiveDays,
        int monthlyTotalDays,
        List<Integer> checkedDays
) {
}
