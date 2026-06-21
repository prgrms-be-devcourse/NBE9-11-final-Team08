package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FeedSseEmitterRegistry {

    private static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(Long studyId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(studyId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(studyId, emitter));
        emitter.onTimeout(() -> {
            remove(studyId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(studyId, emitter));

        sendConnectedEvent(emitter);
        return emitter;
    }

    public void send(FeedOutboxEvent event) {
        List<SseEmitter> studyEmitters = emitters.get(event.getStudyId());
        if (studyEmitters == null || studyEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : studyEmitters) {
            send(emitter, event);
        }
    }

    public void send(SseEmitter emitter, FeedOutboxEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(event.getId()))
                    .name(event.sseEventName())
                    .data(event.getPayload(), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private void sendConnectedEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private void remove(Long studyId, SseEmitter emitter) {
        List<SseEmitter> studyEmitters = emitters.get(studyId);
        if (studyEmitters == null) {
            return;
        }

        studyEmitters.remove(emitter);
        if (studyEmitters.isEmpty()) {
            emitters.remove(studyId);
        }
    }
}
