package com.team08.backend.domain.couponissuerequest.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.coupon-issue-request.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueRequestStreamWorker {

    static final String GROUP_NAME = "coupon-issue-request-workers";
    static final int BATCH_SIZE = 500;

    private final String consumerName = "coupon-issue-request-worker-" + UUID.randomUUID();

    private final StringRedisTemplate redisTemplate;
    private final CouponIssueRequestProcessor couponIssueRequestProcessor;

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    CouponIssueRequestStreamPublisher.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                    GROUP_NAME.getBytes(StandardCharsets.UTF_8),
                    "0".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)
            ));
        } catch (RuntimeException ignored) {
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void processRequests() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, consumerName),
                StreamReadOptions.empty().count(BATCH_SIZE),
                StreamOffset.create(CouponIssueRequestStreamPublisher.STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        List<CouponIssueRequestProcessor.SelectedUserIssueCommand> commands = records.stream()
                .map(this::toCommand)
                .toList();
        couponIssueRequestProcessor.processSelectedUsers(commands);
        redisTemplate.opsForStream().acknowledge(
                CouponIssueRequestStreamPublisher.STREAM_KEY,
                GROUP_NAME,
                records.stream().map(MapRecord::getId).toArray(org.springframework.data.redis.connection.stream.RecordId[]::new)
        );
    }

    private CouponIssueRequestProcessor.SelectedUserIssueCommand toCommand(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        Long requestId = Long.valueOf(String.valueOf(value.get("requestId")));
        Long policyId = Long.valueOf(String.valueOf(value.get("policyId")));
        Long userId = Long.valueOf(String.valueOf(value.get("userId")));
        String issueKey = String.valueOf(value.get("issueKey"));

        return new CouponIssueRequestProcessor.SelectedUserIssueCommand(requestId, policyId, userId, issueKey);
    }
}
