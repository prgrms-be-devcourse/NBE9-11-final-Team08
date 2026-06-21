package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import com.team08.backend.domain.feed.outbox.FeedOutboxEventRepository;
import com.team08.backend.domain.feed.outbox.FeedOutboxEventStatus;
import com.team08.backend.domain.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedSseService {

    private final FeedService feedService;
    private final FeedSseEmitterRegistry feedSseEmitterRegistry;
    private final FeedOutboxEventRepository feedOutboxEventRepository;

    public SseEmitter subscribe(Long studyId, Long userId, Long lastEventId) {
        feedService.validateFeedAccess(studyId, userId);

        SseEmitter emitter = feedSseEmitterRegistry.add(studyId, userId);
        replayMissedEvents(studyId, lastEventId, emitter);
        return emitter;
    }

    private void replayMissedEvents(Long studyId, Long lastEventId, SseEmitter emitter) {
        if (lastEventId == null) {
            return;
        }

        List<FeedOutboxEvent> missedEvents =
                feedOutboxEventRepository.findByStudyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        studyId,
                        FeedOutboxEventStatus.PUBLISHED,
                        lastEventId
                );
        missedEvents.forEach(event -> feedSseEmitterRegistry.send(emitter, event));
    }
}
