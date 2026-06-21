package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;

public record LectureProgressSummary(
        int lastPositionSeconds,
        int watchedSeconds,
        Integer progressRate,
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
