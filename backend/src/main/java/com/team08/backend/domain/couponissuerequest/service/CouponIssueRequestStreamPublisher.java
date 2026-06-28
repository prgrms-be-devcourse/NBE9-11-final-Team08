package com.team08.backend.domain.couponissuerequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
}
