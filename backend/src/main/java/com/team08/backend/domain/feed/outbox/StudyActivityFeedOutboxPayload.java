package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;

import java.time.LocalDateTime;

public record StudyActivityFeedOutboxPayload(
        Long studyActivityId,
        Long studyId,
        Long authorId,
        LocalDateTime createdAt
) {
    public static StudyActivityFeedOutboxPayload from(StudyActivity studyActivity) {
        return new StudyActivityFeedOutboxPayload(
                studyActivity.getId(),
                studyActivity.getStudyId(),
                studyActivity.getAuthorId(),
                studyActivity.getCreatedAt()
        );
    }
}
