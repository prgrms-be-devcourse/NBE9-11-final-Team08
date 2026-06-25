package com.team08.backend.domain.feed.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedOutboxService {

    private final FeedOutboxEventRepository feedOutboxEventRepository;
    private final ObjectMapper objectMapper;

    public FeedOutboxEvent createStudyActivityCreatedEvent(StudyActivityCreated event) {
        StudyActivityFeedOutboxPayload payload = StudyActivityFeedOutboxPayload.from(event);

        try {
            return feedOutboxEventRepository.save(FeedOutboxEvent.studyActivityCreated(
                    payload.studyId(),
                    payload.studyActivityId(),
                    objectMapper.writeValueAsString(payload)
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize study activity event payload.", e);
        }
    }

    public FeedOutboxEvent createLearningEventRecordedEvent(
            LearningEventRecorded event,
            Long studyId,
            String lectureTitle
    ) {
        LearningEventFeedOutboxPayload payload = LearningEventFeedOutboxPayload.from(event, studyId, lectureTitle);

        try {
            return feedOutboxEventRepository.save(FeedOutboxEvent.learningEventRecorded(
                    payload.studyId(),
                    payload.learningEventId(),
                    payload.eventType(),
                    objectMapper.writeValueAsString(payload)
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize learning event payload.", e);
        }
    }
}
