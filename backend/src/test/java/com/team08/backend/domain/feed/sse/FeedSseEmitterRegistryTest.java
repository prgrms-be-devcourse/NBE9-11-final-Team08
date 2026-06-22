package com.team08.backend.domain.feed.sse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class FeedSseEmitterRegistryTest {

    @Test
    void 같은_study_user_연결은_새_emitter로_교체한다() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        FeedSseEmitterRegistry registry = new FeedSseEmitterRegistry(meterRegistry);

        SseEmitter first = registry.add(1L, 10L);
        SseEmitter second = registry.add(1L, 10L);

        assertThat(first).isNotSameAs(second);
        assertThat(registry.connectionCount()).isEqualTo(1);
        assertThat(registry.studyCount()).isEqualTo(1);
        assertThat(meterRegistry.get("feed.sse.connections").gauge().value()).isEqualTo(1.0);
        assertThat(meterRegistry.get("feed.sse.studies").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void 서로_다른_user_연결은_같은_study에_함께_유지한다() {
        FeedSseEmitterRegistry registry = new FeedSseEmitterRegistry(new SimpleMeterRegistry());

        registry.add(1L, 10L);
        registry.add(1L, 20L);

        assertThat(registry.connectionCount()).isEqualTo(2);
        assertThat(registry.studyCount()).isEqualTo(1);
    }

    @Test
    void drainAll은_모든_연결을_닫고_metric을_증가시킨다() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        FeedSseEmitterRegistry registry = new FeedSseEmitterRegistry(meterRegistry);

        registry.add(1L, 10L);
        registry.add(2L, 20L);

        int drainedCount = registry.drainAll("server-draining");

        assertThat(drainedCount).isEqualTo(2);
        assertThat(registry.connectionCount()).isZero();
        assertThat(registry.studyCount()).isZero();
        assertThat(meterRegistry.get("feed.sse.drain.count").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("feed.sse.drained.connections").counter().count()).isEqualTo(2.0);
    }

}
