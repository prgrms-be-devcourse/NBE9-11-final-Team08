package com.team08.backend.domain.couponreward.outbox.stream;

import com.team08.backend.domain.couponreward.outbox.service.CouponRewardOutboxWorker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRewardOutboxStreamWorker {

    private static final String GROUP_NAME = "coupon-reward-outbox-workers";
    private static final long STREAM_WORKER_DELAY_MS = 100;

    private final String consumerName = "coupon-reward-outbox-worker-" + UUID.randomUUID();

    private final StringRedisTemplate redisTemplate;
    private final CouponRewardOutboxWorker couponRewardOutboxWorker;

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    CouponRewardOutboxStreamPublishListener.STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                    GROUP_NAME.getBytes(StandardCharsets.UTF_8),
                    "0".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)
            ));
        } catch (RuntimeException ignored) {
        }
    }

    @Scheduled(fixedDelay = STREAM_WORKER_DELAY_MS)
    public void processEvents() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, consumerName),
                StreamOffset.create(CouponRewardOutboxStreamPublishListener.STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            process(record);
        }
    }

    private void process(MapRecord<String, Object, Object> record) {
        try {
            Map<Object, Object> value = record.getValue();
            Long outboxEventId = Long.valueOf(String.valueOf(value.get("outboxEventId")));
            couponRewardOutboxWorker.processOne(outboxEventId);
            redisTemplate.opsForStream().acknowledge(
                    CouponRewardOutboxStreamPublishListener.STREAM_KEY,
                    GROUP_NAME,
                    record.getId()
            );
        } catch (RuntimeException e) {
            log.warn("쿠폰 보상 outbox Redis Stream 처리 실패. recordId={}", record.getId(), e);
        }
    }
}
