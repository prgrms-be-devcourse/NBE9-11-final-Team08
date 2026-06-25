package com.team08.backend.domain.feed.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.feed.dto.response.FeedItemResponse;
import com.team08.backend.domain.feed.entity.FeedItem;
import com.team08.backend.domain.feed.entity.FeedItemType;
import com.team08.backend.domain.feed.repository.FeedItemRepository;
import com.team08.backend.domain.feed.service.FeedContentSummarizer;
import com.team08.backend.domain.feed.sse.FeedSseConnectionManager;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedOutboxPublisher {

    private static final int PUBLISH_BATCH_SIZE = 100;

    private final FeedOutboxEventRepository feedOutboxEventRepository;
    private final FeedItemRepository feedItemRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final FeedContentSummarizer feedContentSummarizer;
    private final FeedSseConnectionManager feedSseConnectionManager;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final FeedOutboxProperties feedOutboxProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<FeedOutboxEvent> events = feedOutboxEventRepository.findRetryableForUpdateSkipLocked(
                FeedOutboxEventStatus.PENDING.name(),
                FeedOutboxEventStatus.FAILED.name(),
                now,
                PUBLISH_BATCH_SIZE
        );

        for (FeedOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(FeedOutboxEvent event) {
        try {
            prepareFeedItemCreatedPayload(event);
        } catch (DataAccessException | PersistenceException e) {
            throw e;
        } catch (RuntimeException e) {
            LocalDateTime now = LocalDateTime.now(clock);
            event.markFailed(
                    e.getMessage(),
                    now,
                    feedOutboxProperties.maxRetries(),
                    retryDelaySeconds(event.getRetryCount())
            );
            return;
        }

        try {
            feedSseConnectionManager.send(event);
        } catch (RuntimeException ignored) {
        }
    }

    private void prepareFeedItemCreatedPayload(FeedOutboxEvent event) {
        if (event.getFeedItemId() != null) {
            event.markPublished(LocalDateTime.now(clock));
            return;
        }

        if (FeedOutboxEvent.STUDY_ACTIVITY_CREATED_EVENT.equals(event.getEventType())) {
            prepareStudyActivityCreatedPayload(event);
            return;
        }

        if (isLearningEvent(event)) {
            prepareLearningEventPayload(event);
            return;
        }

        throw new IllegalStateException("Unsupported feed outbox event type: " + event.getEventType());
    }

    private void prepareStudyActivityCreatedPayload(FeedOutboxEvent event) {
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

    private void prepareLearningEventPayload(FeedOutboxEvent event) {
        LearningEventFeedOutboxPayload sourceEvent = readLearningEvent(event);
        FeedItemType type = feedItemType(sourceEvent.eventType());
        FeedItem feedItem = feedItemRepository.findByTypeAndSourceId(
                        type,
                        sourceEvent.learningEventId()
                )
                .orElseGet(() -> createFeedItem(sourceEvent, type));

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

    private LearningEventFeedOutboxPayload readLearningEvent(FeedOutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), LearningEventFeedOutboxPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize learning event payload.", e);
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

    private FeedItem createFeedItem(LearningEventFeedOutboxPayload event, FeedItemType type) {
        FeedItem feedItem = FeedItem.createLearningEvent(
                event.studyId(),
                event.actorId(),
                type,
                event.learningEventId(),
                learningEventContent(event),
                event.occurredAt()
        );
        return feedItemRepository.save(feedItem);
    }

    private boolean isLearningEvent(FeedOutboxEvent event) {
        return FeedOutboxEvent.LECTURE_ENTER_EVENT.equals(event.getEventType())
                || FeedOutboxEvent.LECTURE_COMPLETE_EVENT.equals(event.getEventType());
    }

    private FeedItemType feedItemType(com.team08.backend.domain.learningevent.entity.LearningEventType eventType) {
        return switch (eventType) {
            case LECTURE_ENTER -> FeedItemType.LECTURE_ENTER;
            case LECTURE_COMPLETE -> FeedItemType.LECTURE_COMPLETE;
            default -> throw new IllegalStateException("Unsupported learning event type for feed: " + eventType);
        };
    }

    private String learningEventContent(LearningEventFeedOutboxPayload event) {
        return switch (event.eventType()) {
            case LECTURE_ENTER -> "강의에 입장했어요: " + event.lectureTitle();
            case LECTURE_COMPLETE -> "강의를 완료했어요: " + event.lectureTitle();
            default -> throw new IllegalStateException("Unsupported learning event type for feed: " + event.eventType());
        };
    }

    private long retryDelaySeconds(int retryCount) {
        long multiplier = 1L << Math.min(retryCount, 30);
        long delay = feedOutboxProperties.retryBaseDelaySeconds() * multiplier;
        return Math.min(delay, feedOutboxProperties.retryMaxDelaySeconds());
    }
}
