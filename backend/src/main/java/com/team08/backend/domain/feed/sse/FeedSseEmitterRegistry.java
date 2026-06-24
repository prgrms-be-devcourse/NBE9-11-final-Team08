package com.team08.backend.domain.feed.sse;

import com.team08.backend.domain.feed.outbox.FeedOutboxEvent;
import io.micrometer.core.instrument.Counter;
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
public class FeedSseEmitterRegistry implements FeedSseConnectionManager {

    private static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, Map<Long, SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Counter drainCount;
    private final Counter drainedConnections;

    public FeedSseEmitterRegistry(MeterRegistry meterRegistry) {
        Gauge.builder("feed.sse.connections", this, FeedSseEmitterRegistry::connectionCount)
                .description("Current feed SSE connection count")
                .register(meterRegistry);
        Gauge.builder("feed.sse.studies", this, FeedSseEmitterRegistry::studyCount)
                .description("Current study count with feed SSE connections")
                .register(meterRegistry);
        this.drainCount = Counter.builder("feed.sse.drain.count")
                .description("Total feed SSE drain execution count")
                .register(meterRegistry);
        this.drainedConnections = Counter.builder("feed.sse.drained.connections")
                .description("Total feed SSE connections closed by drain")
                .register(meterRegistry);
    }

    @Override
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

    @Override
    public void send(FeedOutboxEvent event) {
        Map<Long, SseEmitter> studyEmitters = emitters.get(event.getStudyId());
        if (studyEmitters == null || studyEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(studyEmitters.values())) {
            send(emitter, event);
        }
    }

    @Override
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

    @Override
    public int drainAll(String reason) {
        drainCount.increment();

        int drainedCount = 0;
        for (Map.Entry<Long, Map<Long, SseEmitter>> studyEntry : List.copyOf(emitters.entrySet())) {
            Long studyId = studyEntry.getKey();
            for (Map.Entry<Long, SseEmitter> userEntry : List.copyOf(studyEntry.getValue().entrySet())) {
                Long userId = userEntry.getKey();
                SseEmitter emitter = userEntry.getValue();
                sendDrainEvent(studyId, userId, emitter, reason);
                remove(studyId, userId, emitter);
                emitter.complete();
                drainedCount++;
            }
        }

        if (drainedCount > 0) {
            drainedConnections.increment(drainedCount);
        }
        return drainedCount;
    }

    @Scheduled(fixedDelayString = "${app.feed.sse.heartbeat-delay-ms:25000}")
    public void sendHeartbeat() {
        emitters.forEach((studyId, studyEmitters) ->
                studyEmitters.forEach((userId, emitter) -> sendHeartbeat(studyId, userId, emitter))
        );
    }

    @Override
    public int connectionCount() {
        return emitters.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    @Override
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

    private void sendDrainEvent(Long studyId, Long userId, SseEmitter emitter, String reason) {
        try {
            emitter.send(SseEmitter.event()
                    .name("server-draining")
                    .data(reason));
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
