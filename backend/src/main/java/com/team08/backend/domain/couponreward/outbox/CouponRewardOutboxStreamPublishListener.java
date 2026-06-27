package com.team08.backend.domain.couponreward.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.coupon-reward.outbox.stream-publish-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CouponRewardOutboxStreamPublishListener {

    public static final String STREAM_KEY = "coupon:reward:outbox-events";

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(CouponRewardOutboxCreatedEvent event) {
        try {
            MapRecord<String, String, String> record = StreamRecords.mapBacked(Map.of(
                            "outboxEventId", String.valueOf(event.outboxEventId())
                    ))
                    .withStreamKey(STREAM_KEY);
            redisTemplate.opsForStream().add(record);
        } catch (RuntimeException e) {
            log.warn("쿠폰 보상 outbox Redis Stream 발행 실패. outboxEventId={}", event.outboxEventId(), e);
        }
    }
}
