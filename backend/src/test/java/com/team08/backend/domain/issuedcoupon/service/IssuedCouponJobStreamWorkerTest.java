package com.team08.backend.domain.issuedcoupon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssuedCouponJobStreamWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private IssuedCouponJobProcessor issuedCouponJobProcessor;

    private IssuedCouponJobStreamWorker streamWorker;

    @BeforeEach
    void setUp() {
        streamWorker = new IssuedCouponJobStreamWorker(redisTemplate, issuedCouponJobProcessor);
    }

    @Test
    @DisplayName("성공: 스트림 레코드를 정상 처리하면 ackCallback을 호출한다")
    void processBatch_success() throws Exception {
        // given
        String requestId = "req-1";
        Long userId = 100L;
        Long policyId = 200L;

        Map<Object, Object> map = Map.of("requestId", requestId, "userId", String.valueOf(userId), "policyId", String.valueOf(policyId));
        MapRecord<String, Object, Object> record = StreamRecords.newRecord()
                .in("stream-key")
                .ofMap(map)
                .withId(RecordId.of("1234-0"));

        Consumer<MapRecord<String, Object, Object>> ackCallback = Mockito.mock(Consumer.class);

        // when
        streamWorker.processBatch(List.of(record), ackCallback);

        // then
        verify(issuedCouponJobProcessor).process(requestId, userId, policyId);
        verify(ackCallback).accept(record);
    }

    @Test
    @DisplayName("성공: 스트림 레코드 처리 중 예외 발생 시 ackCallback을 호출하지 않고 다음으로 넘어간다")
    void processBatch_exception_doesNotAck() throws Exception {
        // given
        String requestId = "req-1";
        Long userId = 100L;
        Long policyId = 200L;

        Map<Object, Object> map1 = Map.of("requestId", requestId, "userId", String.valueOf(userId), "policyId", String.valueOf(policyId));
        MapRecord<String, Object, Object> record1 = StreamRecords.newRecord()
                .in("stream-key")
                .ofMap(map1)
                .withId(RecordId.of("1234-0"));

        String requestId2 = "req-2";
        Long userId2 = 101L;
        Long policyId2 = 201L;

        Map<Object, Object> map2 = Map.of("requestId", requestId2, "userId", String.valueOf(userId2), "policyId", String.valueOf(policyId2));
        MapRecord<String, Object, Object> record2 = StreamRecords.newRecord()
                .in("stream-key")
                .ofMap(map2)
                .withId(RecordId.of("1234-1"));

        Consumer<MapRecord<String, Object, Object>> ackCallback = Mockito.mock(Consumer.class);

        doThrow(new IllegalStateException("Processing error"))
                .when(issuedCouponJobProcessor).process(requestId, userId, policyId);

        // when
        streamWorker.processBatch(List.of(record1, record2), ackCallback);

        // then
        verify(issuedCouponJobProcessor).process(requestId, userId, policyId);
        verify(ackCallback, Mockito.never()).accept(record1);

        verify(issuedCouponJobProcessor).process(requestId2, userId2, policyId2);
        verify(ackCallback).accept(record2);
    }
}
