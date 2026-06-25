package com.team08.backend.domain.dashboard.dto;

/**
 * 강좌별 학습 집계 한 행(드릴다운 1단계).
 * incompletionRate: 0~100, ACTIVE 수강자 중 완강 미달 비율.
 */
public record CourseStatRow(
        Long courseId,
        String title,
        Long instructorId,
        String status,
        long enrollees,
        long enterCount,
        long completionCount,
        double incompletionRate
) {
}
