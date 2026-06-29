package com.team08.backend.domain.couponissuerequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CouponIssueRequestStreamPublisher {

    public static final String STREAM_KEY = "coupon:issue-requests";

    private final StringRedisTemplate redisTemplate;

    public RecordId publish(Long requestId, Long policyId, Long userId, String issueKey) {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(Map.of(
                        "requestId", String.valueOf(requestId),
                        "policyId", String.valueOf(policyId),
                        "userId", String.valueOf(userId),
                        "issueKey", issueKey
                ))
                .withStreamKey(STREAM_KEY);

        return redisTemplate.opsForStream().add(record);
    }

    public void publishAll(Long requestId, Long policyId, List<Long> userIds, String issueKey) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] streamKey = STREAM_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] requestIdValue = String.valueOf(requestId).getBytes(StandardCharsets.UTF_8);
            byte[] policyIdValue = String.valueOf(policyId).getBytes(StandardCharsets.UTF_8);
            byte[] issueKeyValue = issueKey.getBytes(StandardCharsets.UTF_8);

            for (Long userId : userIds) {
                connection.execute(
                        "XADD",
                        streamKey,
                        "*".getBytes(StandardCharsets.UTF_8),
                        "requestId".getBytes(StandardCharsets.UTF_8),
                        requestIdValue,
                        "policyId".getBytes(StandardCharsets.UTF_8),
                        policyIdValue,
                        "userId".getBytes(StandardCharsets.UTF_8),
                        String.valueOf(userId).getBytes(StandardCharsets.UTF_8),
                        "issueKey".getBytes(StandardCharsets.UTF_8),
                        issueKeyValue
                );
            }
            return null;
        });
    }
}
