package com.team08.backend.domain.feed.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedOutboxService {

    private final FeedOutboxEventRepository feedOutboxEventRepository;
    private final ObjectMapper objectMapper;

    public FeedOutboxEvent createStudyActivityCreatedEvent(StudyActivity studyActivity) {
        StudyActivityFeedOutboxPayload payload = StudyActivityFeedOutboxPayload.from(studyActivity);

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
}
