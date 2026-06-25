package com.team08.backend.domain.dashboard.dto;

/**
 * 일별 세션 추이 한 점. 세션 = LECTURE_ENTER 이벤트 수.
 */
public record DailySessionPoint(
        String date,
        long sessions,
        long distinctLearners
) {
}
