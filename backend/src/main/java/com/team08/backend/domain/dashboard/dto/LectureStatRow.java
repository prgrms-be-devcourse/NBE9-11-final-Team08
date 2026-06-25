package com.team08.backend.domain.dashboard.dto;

/**
 * 강의별 학습 집계 한 행(드릴다운 2단계).
 */
public record LectureStatRow(
        Long lectureId,
        String chapterTitle,
        String title,
        long enterCount,
        long completeCount,
        long avgWatchSeconds
) {
}
