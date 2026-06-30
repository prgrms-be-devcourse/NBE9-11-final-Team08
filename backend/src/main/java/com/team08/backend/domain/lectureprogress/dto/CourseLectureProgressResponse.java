package com.team08.backend.domain.lectureprogress.dto;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;

// 강좌 커리큘럼 화면에서 강의별 진행도를 한 번에 내려주기 위한 응답
public record CourseLectureProgressResponse(
        Long lectureId,
        int lastPositionSeconds,
        int progressRate,
        boolean completed
) {
    public static CourseLectureProgressResponse from(LectureProgress progress) {
        return new CourseLectureProgressResponse(
                progress.getLectureId(),
                progress.getLastPositionSeconds(),
                progress.getProgressRate(),
                progress.getCompleted()
        );
    }
}
