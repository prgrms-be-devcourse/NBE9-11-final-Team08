package com.team08.backend.global.redis.stream;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public abstract class AbstractScheduledBatchStreamWorker {

    protected final StringRedisTemplate redisTemplate;
    protected final String consumerName;

    protected AbstractScheduledBatchStreamWorker(StringRedisTemplate redisTemplate, String consumerPrefix) {
        this.redisTemplate = redisTemplate;
        this.consumerName = consumerPrefix + "-" + UUID.randomUUID();
    }

    protected abstract String getStreamKey();

    protected abstract String getGroupName();

    protected abstract int getBatchSize();

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    getStreamKey().getBytes(StandardCharsets.UTF_8),
                    getGroupName().getBytes(StandardCharsets.UTF_8),
                    "0".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)
            ));
        } catch (RuntimeException ignored) {
        }
    }

    protected void pollAndProcess() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                org.springframework.data.redis.connection.stream.Consumer.from(getGroupName(), consumerName),
                StreamReadOptions.empty().count(getBatchSize()),
                StreamOffset.create(getStreamKey(), ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        try {
            processBatch(records, this::ack);
        } catch (Exception e) {
            handleBatchError(records, e);
        }
    }

    protected abstract void processBatch(
            List<MapRecord<String, Object, Object>> records,
            java.util.function.Consumer<MapRecord<String, Object, Object>> ackCallback
    ) throws Exception;

    protected void ack(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(
                getStreamKey(),
                getGroupName(),
                record.getId()
        );
    }

    protected void handleBatchError(List<MapRecord<String, Object, Object>> records, Exception e) {
        // 인프라 레벨의 전체 배치 실패 시 ACK하지 않고 PEL에 남겨둡니다.
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Redis Stream Scheduled Batch 처리 중 예외 발생. (PEL 대기) stream={}, group={}, batchSize={}",
                getStreamKey(), getGroupName(), records.size(), e);
    }
}
