package com.team08.backend.domain.issuedcouponjob.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobStreamPublisher {

    public static final String STREAM_KEY = "coupon:fcfs:issue-jobs";

    private final StringRedisTemplate redisTemplate;

    // 선착순 쿠폰 발급 작업 적재
    public RecordId publish(Long jobId, Long userId, Long policyId) {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(Map.of(
                        "jobId", String.valueOf(jobId),
                        "userId", String.valueOf(userId),
                        "policyId", String.valueOf(policyId)
                ))
                .withStreamKey(STREAM_KEY);

        return redisTemplate.opsForStream().add(record);
    }
}
