package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import com.team08.backend.domain.feed.outbox.FeedOutboxEventRepository;
import com.team08.backend.domain.feed.outbox.FeedOutboxEventStatus;
import com.team08.backend.domain.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedSseServiceTest {

    @Mock
    private FeedService feedService;

    @Mock
    private FeedSseConnectionManager feedSseConnectionManager;

    @Mock
    private FeedOutboxEventRepository feedOutboxEventRepository;

    @Test
    void subscribe는_인터페이스로_연결하고_lastEventId_이후_이벤트를_replay한다() {
        FeedSseService feedSseService = new FeedSseService(
                feedService,
                feedSseConnectionManager,
                feedOutboxEventRepository
        );
        Long studyId = 1L;
        Long userId = 10L;
        Long lastEventId = 100L;
        SseEmitter emitter = new SseEmitter();
        FeedOutboxEvent event = FeedOutboxEvent.studyActivityCreated(studyId, 20L, "{}");

        given(feedSseConnectionManager.add(studyId, userId)).willReturn(emitter);
        given(feedOutboxEventRepository.findByStudyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                studyId,
                FeedOutboxEventStatus.PUBLISHED,
                lastEventId
        )).willReturn(List.of(event));

        feedSseService.subscribe(studyId, userId, lastEventId);

        verify(feedService).validateFeedAccess(studyId, userId);
        verify(feedSseConnectionManager).add(studyId, userId);
        verify(feedSseConnectionManager).send(emitter, event);
    }
}
