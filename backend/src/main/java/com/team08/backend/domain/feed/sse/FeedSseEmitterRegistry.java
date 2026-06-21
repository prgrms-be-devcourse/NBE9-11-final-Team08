package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FeedSseEmitterRegistry {

    private static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, Map<Long, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public FeedSseEmitterRegistry(MeterRegistry meterRegistry) {
        Gauge.builder("feed.sse.connections", this, FeedSseEmitterRegistry::connectionCount)
                .description("Current feed SSE connection count")
                .register(meterRegistry);
        Gauge.builder("feed.sse.studies", this, FeedSseEmitterRegistry::studyCount)
                .description("Current study count with feed SSE connections")
                .register(meterRegistry);
    }

    public SseEmitter add(Long studyId, Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        Map<Long, SseEmitter> studyEmitters =
                emitters.computeIfAbsent(studyId, ignored -> new ConcurrentHashMap<>());
        SseEmitter previousEmitter = studyEmitters.put(userId, emitter);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }

        emitter.onCompletion(() -> remove(studyId, userId, emitter));
        emitter.onTimeout(() -> {
            remove(studyId, userId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(studyId, userId, emitter));

        sendConnectedEvent(studyId, userId, emitter);
        return emitter;
    }

    public void send(FeedOutboxEvent event) {
        Map<Long, SseEmitter> studyEmitters = emitters.get(event.getStudyId());
        if (studyEmitters == null || studyEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(studyEmitters.values())) {
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

    @Scheduled(fixedDelayString = "${app.feed.sse.heartbeat-delay-ms:25000}")
    public void sendHeartbeat() {
        emitters.forEach((studyId, studyEmitters) ->
                studyEmitters.forEach((userId, emitter) -> sendHeartbeat(studyId, userId, emitter))
        );
    }

    public int connectionCount() {
        return emitters.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    public int studyCount() {
        return emitters.size();
    }

    private void sendConnectedEvent(Long studyId, Long userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException | IllegalStateException e) {
            remove(studyId, userId, emitter);
            emitter.completeWithError(e);
        }
    }

    private void sendHeartbeat(Long studyId, Long userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (IOException | IllegalStateException e) {
            remove(studyId, userId, emitter);
            emitter.completeWithError(e);
        }
    }

    private void remove(Long studyId, Long userId, SseEmitter emitter) {
        Map<Long, SseEmitter> studyEmitters = emitters.get(studyId);
        if (studyEmitters == null) {
            return;
        }

        studyEmitters.remove(userId, emitter);
        if (studyEmitters.isEmpty()) {
            emitters.remove(studyId, studyEmitters);
        }
    }
}
