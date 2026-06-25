package com.team08.backend.domain.studyactivity.event;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;

import java.time.LocalDateTime;

public record StudyActivityCreated(
        Long studyActivityId,
        Long studyId,
        Long authorId,
        LocalDateTime createdAt
) {
    public static StudyActivityCreated from(StudyActivity studyActivity) {
        return new StudyActivityCreated(
                studyActivity.getId(),
                studyActivity.getStudyId(),
                studyActivity.getAuthorId(),
                studyActivity.getCreatedAt()
        );
    }
}
