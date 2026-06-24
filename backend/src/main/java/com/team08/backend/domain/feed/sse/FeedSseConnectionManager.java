package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface FeedSseConnectionManager {

    SseEmitter add(Long studyId, Long userId);

    void send(FeedOutboxEvent event);

    void send(SseEmitter emitter, FeedOutboxEvent event);

    int drainAll(String reason);

    int connectionCount();

    int studyCount();
}
