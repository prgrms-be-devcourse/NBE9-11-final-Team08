package com.team08.backend.domain.feed.outbox;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedOutboxPublisherTest {

    @Mock
    private FeedOutboxEventRepository feedOutboxEventRepository;

    @Mock
    private FeedItemRepository feedItemRepository;

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private FeedContentSummarizer feedContentSummarizer;

    @Mock
    private FeedSseEmitterRegistry feedSseEmitterRegistry;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-21T04:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private FeedOutboxPublisher feedOutboxPublisher;

    @BeforeEach
    void setUp() {
        feedOutboxPublisher = new FeedOutboxPublisher(
                feedOutboxEventRepository,
                feedItemRepository,
                studyActivityRepository,
                feedContentSummarizer,
                feedSseEmitterRegistry,
                userRepository,
                objectMapper,
                clock
        );
    }

    @Test
    void pending_outbox를_feedItem으로_변환하고_SSE로_발행한다() throws Exception {
        Long studyId = 1L;
        Long activityId = 10L;
        Long authorId = 3L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 21, 13, 0);
        StudyActivity activity = StudyActivity.create(studyId, authorId, "긴 활동 내용");
        ReflectionTestUtils.setField(activity, "id", activityId);
        ReflectionTestUtils.setField(activity, "createdAt", createdAt);
        StudyActivityFeedOutboxPayload sourceEvent = StudyActivityFeedOutboxPayload.from(activity);
        FeedOutboxEvent outboxEvent = FeedOutboxEvent.studyActivityCreated(
                studyId,
                activityId,
                objectMapper.writeValueAsString(sourceEvent)
        );
        ReflectionTestUtils.setField(outboxEvent, "id", 100L);

        User user = User.createUser("user@test.com", "password", "테스터", null);
        ReflectionTestUtils.setField(user, "id", authorId);

        given(feedOutboxEventRepository.findRetryableForUpdateSkipLocked(
                List.of("PENDING", "FAILED"),
                100
        )).willReturn(List.of(outboxEvent));
        given(feedItemRepository.findByTypeAndSourceId(FeedItemType.STUDY_ACTIVITY, activityId))
                .willReturn(Optional.empty());
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId))
                .willReturn(Optional.of(activity));
        given(feedContentSummarizer.summarize("긴 활동 내용"))
                .willReturn("요약된 활동 내용");
        given(feedItemRepository.save(org.mockito.ArgumentMatchers.any(FeedItem.class)))
                .willAnswer(invocation -> {
                    FeedItem feedItem = invocation.getArgument(0);
                    ReflectionTestUtils.setField(feedItem, "id", 200L);
                    return feedItem;
                });
        given(userRepository.findById(authorId)).willReturn(Optional.of(user));

        feedOutboxPublisher.publishPending();

        ArgumentCaptor<FeedItem> feedItemCaptor = ArgumentCaptor.forClass(FeedItem.class);
        verify(feedItemRepository).save(feedItemCaptor.capture());

        FeedItem savedFeedItem = feedItemCaptor.getValue();
        assertThat(savedFeedItem.getStudyId()).isEqualTo(studyId);
        assertThat(savedFeedItem.getActorId()).isEqualTo(authorId);
        assertThat(savedFeedItem.getSourceId()).isEqualTo(activityId);
        assertThat(savedFeedItem.getContent()).isEqualTo("요약된 활동 내용");

        assertThat(outboxEvent.getFeedItemId()).isEqualTo(200L);
        assertThat(outboxEvent.getEventType()).isEqualTo(FeedOutboxEvent.STUDY_ACTIVITY_CREATED_EVENT);
        assertThat(outboxEvent.sseEventName()).isEqualTo(FeedOutboxEvent.FEED_ITEM_CREATED_EVENT);
        assertThat(outboxEvent.getStatus()).isEqualTo(FeedOutboxEventStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(outboxEvent.getPayload()).contains("\"actorNickname\":\"테스터\"");
        assertThat(objectMapper.writeValueAsString(sourceEvent)).doesNotContain("긴 활동 내용");

        verify(feedSseEmitterRegistry).send(outboxEvent);
    }

    @Test
    void failed_outbox도_재시도하고_이미_feedItem_payload면_다시_생성하지_않는다() throws Exception {
        Long studyId = 1L;
        Long activityId = 10L;
        Long feedItemId = 200L;
        Long authorId = 3L;
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 21, 13, 0);

        FeedItem feedItem = FeedItem.createStudyActivity(
                studyId,
                authorId,
                activityId,
                "요약된 활동 내용",
                occurredAt
        );
        ReflectionTestUtils.setField(feedItem, "id", feedItemId);
        FeedOutboxEvent outboxEvent = FeedOutboxEvent.studyActivityCreated(
                studyId,
                activityId,
                objectMapper.writeValueAsString(new FeedItemResponse(
                        feedItemId,
                        studyId,
                        authorId,
                        "테스터",
                        FeedItemType.STUDY_ACTIVITY,
                        activityId,
                        "요약된 활동 내용",
                        occurredAt
                ))
        );
        ReflectionTestUtils.setField(outboxEvent, "id", 100L);
        outboxEvent.markPublished(feedItemId, outboxEvent.getPayload(), occurredAt);
        outboxEvent.markFailed("temporary failure");

        given(feedOutboxEventRepository.findRetryableForUpdateSkipLocked(
                List.of("PENDING", "FAILED"),
                100
        )).willReturn(List.of(outboxEvent));

        feedOutboxPublisher.publishPending();

        assertThat(outboxEvent.getStatus()).isEqualTo(FeedOutboxEventStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(outboxEvent.getLastError()).isNull();
        verify(feedItemRepository, never()).save(org.mockito.ArgumentMatchers.any(FeedItem.class));
        verify(studyActivityRepository, never()).findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId);
        verify(feedSseEmitterRegistry).send(outboxEvent);
    }

    @Test
    void SSE_전송이_실패해도_outbox는_published로_유지한다() throws Exception {
        Long studyId = 1L;
        Long activityId = 10L;
        Long authorId = 3L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 21, 13, 0);
        StudyActivity activity = StudyActivity.create(studyId, authorId, "긴 활동 내용");
        ReflectionTestUtils.setField(activity, "id", activityId);
        ReflectionTestUtils.setField(activity, "createdAt", createdAt);
        StudyActivityFeedOutboxPayload sourceEvent = StudyActivityFeedOutboxPayload.from(activity);
        FeedOutboxEvent outboxEvent = FeedOutboxEvent.studyActivityCreated(
                studyId,
                activityId,
                objectMapper.writeValueAsString(sourceEvent)
        );
        ReflectionTestUtils.setField(outboxEvent, "id", 100L);

        User user = User.createUser("user@test.com", "password", "테스터", null);
        ReflectionTestUtils.setField(user, "id", authorId);

        given(feedOutboxEventRepository.findRetryableForUpdateSkipLocked(
                List.of("PENDING", "FAILED"),
                100
        )).willReturn(List.of(outboxEvent));
        given(feedItemRepository.findByTypeAndSourceId(FeedItemType.STUDY_ACTIVITY, activityId))
                .willReturn(Optional.empty());
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId))
                .willReturn(Optional.of(activity));
        given(feedContentSummarizer.summarize("긴 활동 내용"))
                .willReturn("요약된 활동 내용");
        given(feedItemRepository.save(org.mockito.ArgumentMatchers.any(FeedItem.class)))
                .willAnswer(invocation -> {
                    FeedItem feedItem = invocation.getArgument(0);
                    ReflectionTestUtils.setField(feedItem, "id", 200L);
                    return feedItem;
                });
        given(userRepository.findById(authorId)).willReturn(Optional.of(user));
        willThrow(new RuntimeException("sse failed"))
                .given(feedSseEmitterRegistry)
                .send(outboxEvent);

        feedOutboxPublisher.publishPending();

        assertThat(outboxEvent.getStatus()).isEqualTo(FeedOutboxEventStatus.PUBLISHED);
        assertThat(outboxEvent.getLastError()).isNull();
        assertThat(outboxEvent.getFeedItemId()).isEqualTo(200L);
    }
}
