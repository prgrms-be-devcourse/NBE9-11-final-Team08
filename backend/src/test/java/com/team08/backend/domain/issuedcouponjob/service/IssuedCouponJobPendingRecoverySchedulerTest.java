package com.team08.backend.domain.issuedcouponjob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.global.redis.stream.AbstractPendingRecoveryScheduler.ClaimedRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssuedCouponJobPendingRecoverySchedulerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IssuedCouponJobProcessor issuedCouponJobProcessor;

    private IssuedCouponJobPendingRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new IssuedCouponJobPendingRecoveryScheduler(redisTemplate, objectMapper, issuedCouponJobProcessor);
    }

    @Test
    @DisplayName("성공: Claim된 레코드를 정상 복구 처리하면 ackCallback을 호출한다")
    void processRetryRecords_success() throws Exception {
        // given
        String recordId = "1234-0";
        String requestId = "req-1";
        Long userId = 100L;
        Long policyId = 200L;

        ClaimedRecord record = new ClaimedRecord(
                recordId,
                Map.of("requestId", requestId, "userId", String.valueOf(userId), "policyId", String.valueOf(policyId)),
                1L
        );

        Consumer<String> ackCallback = Mockito.mock(Consumer.class);

        // when
        scheduler.processRetryRecords(List.of(record), ackCallback);

        // then
        verify(issuedCouponJobProcessor).process(requestId, userId, policyId);
        verify(ackCallback).accept(recordId);
    }

    @Test
    @DisplayName("성공: 레코드 복구 처리 중 예외 발생 시 ackCallback을 호출하지 않고 다음으로 넘어간다")
    void processRetryRecords_exception_doesNotAck() throws Exception {
        // given
        String recordId1 = "1234-0";
        String requestId1 = "req-1";
        Long userId1 = 100L;
        Long policyId1 = 200L;

        ClaimedRecord record1 = new ClaimedRecord(
                recordId1,
                Map.of("requestId", requestId1, "userId", String.valueOf(userId1), "policyId", String.valueOf(policyId1)),
                1L
        );

        String recordId2 = "1234-1";
        String requestId2 = "req-2";
        Long userId2 = 101L;
        Long policyId2 = 201L;

        ClaimedRecord record2 = new ClaimedRecord(
                recordId2,
                Map.of("requestId", requestId2, "userId", String.valueOf(userId2), "policyId", String.valueOf(policyId2)),
                1L
        );

        Consumer<String> ackCallback = Mockito.mock(Consumer.class);

        doThrow(new IllegalStateException("Processing error"))
                .when(issuedCouponJobProcessor).process(requestId1, userId1, policyId1);

        // when
        scheduler.processRetryRecords(List.of(record1, record2), ackCallback);

        // then
        verify(issuedCouponJobProcessor).process(requestId1, userId1, policyId1);
        verify(ackCallback, Mockito.never()).accept(recordId1);

        verify(issuedCouponJobProcessor).process(requestId2, userId2, policyId2);
        verify(ackCallback).accept(recordId2);
    }
}
