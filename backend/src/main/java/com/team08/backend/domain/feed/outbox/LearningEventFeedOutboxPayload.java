package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;

import java.time.LocalDateTime;

public record LearningEventFeedOutboxPayload(
        Long learningEventId,
        Long studyId,
        Long actorId,
        Long lectureId,
        String lectureTitle,
        LearningEventType eventType,
        LocalDateTime occurredAt
) {
    public static LearningEventFeedOutboxPayload from(
            LearningEventRecorded event,
            Long studyId,
            String lectureTitle
    ) {
        return new LearningEventFeedOutboxPayload(
                event.learningEventId(),
                studyId,
                event.userId(),
                event.lectureId(),
                lectureTitle,
                event.eventType(),
                event.eventTime()
        );
    }
}
