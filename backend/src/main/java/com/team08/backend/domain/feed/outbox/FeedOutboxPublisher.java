package com.team08.backend.domain.feed.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.feed.service.FeedContentSummarizer;
import com.team08.backend.domain.feed.sse.FeedSseEmitterRegistry;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedOutboxPublisher {

    private static final int PUBLISH_BATCH_SIZE = 100;
    private static final List<FeedOutboxEventStatus> RETRYABLE_STATUSES = List.of(
            FeedOutboxEventStatus.PENDING,
            FeedOutboxEventStatus.FAILED
    );

    private final FeedOutboxEventRepository feedOutboxEventRepository;
    private final FeedItemRepository feedItemRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final FeedContentSummarizer feedContentSummarizer;
    private final FeedSseEmitterRegistry feedSseEmitterRegistry;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPending() {
        List<FeedOutboxEvent> events = feedOutboxEventRepository.findByStatusInOrderByIdAsc(
                RETRYABLE_STATUSES,
                PageRequest.of(0, PUBLISH_BATCH_SIZE)
        );

        for (FeedOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(FeedOutboxEvent event) {
        try {
            prepareFeedItemCreatedPayload(event);
        } catch (RuntimeException e) {
            event.markFailed(e.getMessage());
            return;
        }

        try {
            feedSseEmitterRegistry.send(event);
        } catch (RuntimeException ignored) {
        }
    }

    private void prepareFeedItemCreatedPayload(FeedOutboxEvent event) {
        if (event.getFeedItemId() != null) {
            event.markPublished(LocalDateTime.now(clock));
            return;
        }

        if (!FeedOutboxEvent.STUDY_ACTIVITY_CREATED_EVENT.equals(event.getEventType())) {
            return;
        }

        StudyActivityFeedOutboxPayload sourceEvent = readStudyActivityCreatedEvent(event);
        FeedItem feedItem = feedItemRepository.findByTypeAndSourceId(
                        FeedItemType.STUDY_ACTIVITY,
                        sourceEvent.studyActivityId()
                )
                .orElseGet(() -> createFeedItem(sourceEvent));

        String actorNickname = userRepository.findById(feedItem.getActorId())
                .map(User::getNickname)
                .orElse("알 수 없는 사용자");
        FeedItemResponse payload = FeedItemResponse.from(feedItem, actorNickname);

        try {
            event.markPublished(
                    feedItem.getId(),
                    objectMapper.writeValueAsString(payload),
                    LocalDateTime.now(clock)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize feed item event payload.", e);
        }
    }

    private StudyActivityFeedOutboxPayload readStudyActivityCreatedEvent(FeedOutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), StudyActivityFeedOutboxPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize study activity event payload.", e);
        }
    }

    private FeedItem createFeedItem(StudyActivityFeedOutboxPayload event) {
        StudyActivity activity = studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                        event.studyActivityId(),
                        event.studyId()
                )
                .orElseThrow(() -> new IllegalStateException("Study activity not found for feed outbox event."));

        String summaryContent = feedContentSummarizer.summarize(activity.getContent());
        FeedItem feedItem = FeedItem.createStudyActivity(
                event.studyId(),
                event.authorId(),
                event.studyActivityId(),
                summaryContent,
                event.createdAt()
        );
        return feedItemRepository.save(feedItem);
    }
}
