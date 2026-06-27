package com.team08.backend.domain.couponreward.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxStreamWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private CouponRewardOutboxWorker couponRewardOutboxWorker;

    @Mock
    private MapRecord<String, Object, Object> record;

    @Test
    @DisplayName("Redis Stream 메시지를 처리하고 성공하면 ACK 한다")
    void processEvents_processesRecordAndAcknowledgesWhenSuccessful() {
        // given
        CouponRewardOutboxStreamWorker worker = new CouponRewardOutboxStreamWorker(redisTemplate, couponRewardOutboxWorker);
        RecordId recordId = RecordId.of("1-0");
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
        given(streamOperations.read(any(Consumer.class), any(StreamOffset.class))).willReturn(List.of(record));
        given(record.getValue()).willReturn(Map.of("outboxEventId", "10"));
        given(record.getId()).willReturn(recordId);

        // when
        worker.processEvents();

        // then
        then(couponRewardOutboxWorker).should().processOne(10L);
        then(streamOperations).should().acknowledge(
                CouponRewardOutboxStreamPublishListener.STREAM_KEY,
                "coupon-reward-outbox-workers",
                recordId
        );
    }

    @Test
    @DisplayName("메시지 처리 실패 시 ACK 하지 않는다")
    void processEvents_doesNotAcknowledgeWhenProcessingFails() {
        // given
        CouponRewardOutboxStreamWorker worker = new CouponRewardOutboxStreamWorker(redisTemplate, couponRewardOutboxWorker);
        RecordId recordId = RecordId.of("1-0");
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
        given(streamOperations.read(any(Consumer.class), any(StreamOffset.class))).willReturn(List.of(record));
        given(record.getValue()).willReturn(Map.of("outboxEventId", "10"));
        given(record.getId()).willReturn(recordId);
        willThrow(new IllegalStateException("failed")).given(couponRewardOutboxWorker).processOne(10L);

        // when
        worker.processEvents();

        // then
        then(streamOperations).should(never()).acknowledge(
                CouponRewardOutboxStreamPublishListener.STREAM_KEY,
                "coupon-reward-outbox-workers",
                recordId
        );
    }
}
