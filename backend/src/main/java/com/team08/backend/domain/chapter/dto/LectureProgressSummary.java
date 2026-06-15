package com.team08.backend.domain.chapter.dto;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;

import java.math.BigDecimal;

public record LectureProgressSummary(
        int lastPositionSeconds,
        int watchedSeconds,
        BigDecimal progressRate,
        boolean completed
) {
    public static LectureProgressSummary from(LectureProgress progress) {
        return new LectureProgressSummary(
                progress.getLastPositionSeconds(),
                progress.getWatchedSeconds(),
                progress.getProgressRate(),
                progress.getCompleted()
        );
    }
}
