package com.team08.backend.domain.issuedcouponjob.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IssuedCouponJobStreamWorker {

    private static final String GROUP_NAME = "coupon-issue-workers";
    private static final String CONSUMER_NAME = "coupon-issue-worker-1";

    private final StringRedisTemplate redisTemplate;
    private final IssuedCouponJobProcessor issuedCouponJobProcessor;

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    IssuedCouponJobStreamPublisher.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                    GROUP_NAME.getBytes(StandardCharsets.UTF_8),
                    "0".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)
            ));
        } catch (RuntimeException ignored) {
        }
    }

    // 선착순 쿠폰 발급 작업 처리
    @Scheduled(fixedDelay = 1000)
    public void processJobs() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                StreamOffset.create(IssuedCouponJobStreamPublisher.STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            process(record);
            redisTemplate.opsForStream().acknowledge(
                    IssuedCouponJobStreamPublisher.STREAM_KEY,
                    GROUP_NAME,
                    record.getId()
            );
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        Long jobId = Long.valueOf(String.valueOf(value.get("jobId")));
        issuedCouponJobProcessor.process(jobId);
    }
}
