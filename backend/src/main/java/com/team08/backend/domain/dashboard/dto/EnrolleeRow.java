package com.team08.backend.domain.dashboard.dto;

import java.time.LocalDateTime;

/**
 * 수강자별 진행 현황 한 행(드릴다운 3단계).
 */
public record EnrolleeRow(
        Long userId,
        String nickname,
        long completedLectures,
        long totalLectures,
        double progressRate,
        LocalDateTime lastEventTime
) {
}
