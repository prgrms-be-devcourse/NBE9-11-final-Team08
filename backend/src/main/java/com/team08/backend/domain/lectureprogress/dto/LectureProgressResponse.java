package com.team08.backend.domain.lectureprogress.dto;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;

public record LectureProgressResponse(
        Long lectureId,
        Integer lastPositionSeconds,
        Integer watchedSeconds,
        Integer progressRate,
        Boolean completed
) {
    public static LectureProgressResponse from(LectureProgress progress) {
        return new LectureProgressResponse(
                progress.getLectureId(),
                progress.getLastPositionSeconds(),
                progress.getWatchedSeconds(),
                progress.getProgressRate(),
                progress.getCompleted()
        );
    }
}
