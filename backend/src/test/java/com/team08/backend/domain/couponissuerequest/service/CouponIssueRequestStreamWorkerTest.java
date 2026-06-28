package com.team08.backend.domain.couponissuerequest.service;

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
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponIssueRequestStreamWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private CouponIssueRequestProcessor couponIssueRequestProcessor;

    @Mock
    private MapRecord<String, Object, Object> record;

    @Test
    @DisplayName("Redis Stream 메시지를 읽어 특정 회원 쿠폰 발급을 처리하고 ACK 한다")
    void processRequests_processesRecordAndAcknowledges() {
        // given
        CouponIssueRequestStreamWorker worker = new CouponIssueRequestStreamWorker(
                redisTemplate,
                couponIssueRequestProcessor
        );
        RecordId recordId = RecordId.of("1-0");
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
        given(streamOperations.read(any(Consumer.class), any(StreamOffset.class))).willReturn(List.of(record));
        given(record.getValue()).willReturn(Map.of(
                "requestId", "100",
                "policyId", "10",
                "userId", "1",
                "issueKey", "SELECTED_USERS_VIP_EVENT_2026"
        ));
        given(record.getId()).willReturn(recordId);

        // when
        worker.processRequests();

        // then
        then(couponIssueRequestProcessor).should()
                .processSelectedUser(100L, 10L, 1L, "SELECTED_USERS_VIP_EVENT_2026");
        then(streamOperations).should().acknowledge(
                CouponIssueRequestStreamPublisher.STREAM_KEY,
                "coupon-issue-request-workers",
                recordId
        );
    }

    @Test
    @DisplayName("처리할 Redis Stream 메시지가 없으면 아무 작업도 하지 않는다")
    void processRequests_doesNothingWhenRecordEmpty() {
        // given
        CouponIssueRequestStreamWorker worker = new CouponIssueRequestStreamWorker(
                redisTemplate,
                couponIssueRequestProcessor
        );
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
        given(streamOperations.read(any(Consumer.class), any(StreamOffset.class))).willReturn(List.of());

        // when
        worker.processRequests();

        // then
        then(couponIssueRequestProcessor).shouldHaveNoInteractions();
    }
}
