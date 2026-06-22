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
    private final FeedSseConnectionManager feedSseConnectionManager;
    private final FeedOutboxEventRepository feedOutboxEventRepository;

    public SseEmitter subscribe(Long studyId, Long userId, String lastEventId) {
        feedService.validateFeedAccess(studyId, userId);

        SseEmitter emitter = feedSseConnectionManager.add(studyId, userId);
        replayMissedEvents(studyId, parseLastEventId(lastEventId), emitter);
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
        missedEvents.forEach(event -> feedSseConnectionManager.send(emitter, event));
    }

    private Long parseLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
