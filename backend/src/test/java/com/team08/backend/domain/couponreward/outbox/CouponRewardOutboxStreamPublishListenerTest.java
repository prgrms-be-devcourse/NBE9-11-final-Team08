package com.team08.backend.domain.couponreward.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxStreamPublishListenerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    @DisplayName("커밋 후 이벤트를 Redis Stream에 outboxEventId로 발행한다")
    void publish_addsOutboxEventIdToRedisStream() {
        // given
        CouponRewardOutboxStreamPublishListener listener = new CouponRewardOutboxStreamPublishListener(redisTemplate);
        given(redisTemplate.opsForStream()).willReturn(streamOperations);

        // when
        listener.publish(new CouponRewardOutboxCreatedEvent(10L));

        // then
        ArgumentCaptor<MapRecord<String, String, String>> recordCaptor = ArgumentCaptor.forClass(MapRecord.class);
        then(streamOperations).should().add(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getStream()).isEqualTo(CouponRewardOutboxStreamPublishListener.STREAM_KEY);
        assertThat(recordCaptor.getValue().getValue()).containsEntry("outboxEventId", "10");
    }

    @Test
    @DisplayName("Redis Stream 발행 실패는 바깥으로 전파하지 않는다")
    void publish_doesNotThrowWhenRedisPublishFails() {
        // given
        CouponRewardOutboxStreamPublishListener listener = new CouponRewardOutboxStreamPublishListener(redisTemplate);
        given(redisTemplate.opsForStream()).willThrow(new IllegalStateException("redis down"));

        // when
        listener.publish(new CouponRewardOutboxCreatedEvent(10L));

        // then
        then(redisTemplate).should().opsForStream();
    }
}
