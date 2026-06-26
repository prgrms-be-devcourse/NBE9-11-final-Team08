package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;

import java.time.LocalDateTime;

public record StudyActivityFeedOutboxPayload(
        Long studyActivityId,
        Long studyId,
        Long authorId,
        LocalDateTime createdAt
) {
    public static StudyActivityFeedOutboxPayload from(StudyActivityCreated event) {
        return new StudyActivityFeedOutboxPayload(
                event.studyActivityId(),
                event.studyId(),
                event.authorId(),
                event.createdAt()
        );
    }
}
