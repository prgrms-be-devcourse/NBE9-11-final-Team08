package com.team08.backend.domain.studyactivity.dto;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;

import java.time.LocalDateTime;

public record StudyActivityResponse(
        Long activityId,
        Long studyId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt
) {
    public static StudyActivityResponse from(StudyActivity activity, String authorNickname) {
        return new StudyActivityResponse(
                activity.getId(),
                activity.getStudyId(),
                activity.getAuthorId(),
                authorNickname,
                activity.getContent(),
                activity.getCreatedAt()
        );
    }
}
