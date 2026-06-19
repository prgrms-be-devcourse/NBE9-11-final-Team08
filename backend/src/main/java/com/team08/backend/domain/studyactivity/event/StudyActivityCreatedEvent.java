package com.team08.backend.domain.studyactivity.event;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;

import java.time.LocalDateTime;

public record StudyActivityCreatedEvent(
        Long studyActivityId,
        Long studyId,
        Long authorId,
        String content,
        LocalDateTime createdAt
) {
    public static StudyActivityCreatedEvent from(StudyActivity studyActivity) {
        return new StudyActivityCreatedEvent(
                studyActivity.getId(),
                studyActivity.getStudyId(),
                studyActivity.getAuthorId(),
                studyActivity.getContent(),
                studyActivity.getCreatedAt()
        );
    }
}
