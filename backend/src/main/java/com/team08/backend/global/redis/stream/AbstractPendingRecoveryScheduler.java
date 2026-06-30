package com.team08.backend.global.redis.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Redis Stream의 PEL(Pending Entries List)을 스캔하여 재처리하거나 DLQ로 격리하는 공통 스케줄러.
 * @Scheduled 어노테이션이 붙은 하위 클래스의 메서드에서 주기적으로 호출합니다.
 */
public abstract class AbstractPendingRecoveryScheduler {

    protected final StringRedisTemplate redisTemplate;
    protected final ObjectMapper objectMapper;
    private final String recoveryConsumerName;

    protected AbstractPendingRecoveryScheduler(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, String consumerPrefix) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.recoveryConsumerName = consumerPrefix + "-recovery-" + UUID.randomUUID();
    }

    protected abstract String getStreamKey();
    protected abstract String getGroupName();
    
    /**
     * Pending 상태로 얼마나 머물러야 복구 대상으로 간주할지 설정합니다.
     */
    protected Duration getMinIdleTime() {
        return Duration.ofMinutes(5);
    }

    /**
     * 최대 몇 번 재시도 후 DLQ로 보낼지 설정합니다. (기본 3회)
     */
    protected int getMaxRetries() {
        return 3;
    }

    protected int getClaimCount() {
        return 500;
    }

    /**
     * @Scheduled 어노테이션이 붙은 하위 클래스의 메서드에서 이 메서드를 주기적으로 호출합니다.
     */
    protected void recoverPendingRecords() {
        Map<String, Long> pendingRecordDeliveryCounts = findPendingRecordDeliveryCounts();
        if (pendingRecordDeliveryCounts.isEmpty()) {
            return;
        }

        List<String> recordIdsToClaim = new ArrayList<>(pendingRecordDeliveryCounts.keySet());
        List<ClaimedRecord> claimedRecords = claimPendingRecords(recordIdsToClaim, pendingRecordDeliveryCounts);
        if (claimedRecords.isEmpty()) {
            return;
        }

        List<ClaimedRecord> retryRecords = new ArrayList<>();

        for (ClaimedRecord record : claimedRecords) {
            if (record.deliveryCount() > getMaxRetries()) {
                handlePoisonMessage(record);
            } else {
                retryRecords.add(record);
            }
        }

        if (!retryRecords.isEmpty()) {
            try {
                processRetryRecords(retryRecords, this::ack);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                        "Recovery Scheduler 재처리 중 예외 발생. (PEL 대기) stream={}", getStreamKey(), e);
            }
        }
    }

    /**
     * 재시도 가능한 레코드들을 비즈니스 로직에 넘깁니다.
     * 성공적으로 처리된 레코드에 대해서만 ackCallback.accept(recordId)를 호출해야 합니다.
     */
    protected abstract void processRetryRecords(List<ClaimedRecord> records, Consumer<String> ackCallback) throws Exception;

    protected void ack(String recordId) {
        redisTemplate.opsForStream().acknowledge(
                getStreamKey(),
                getGroupName(),
                RecordId.of(recordId)
        );
    }

    private void handlePoisonMessage(ClaimedRecord record) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error(
                "Poison Message 감지됨. DLQ 격리 시작. stream={}, recordId={}, deliveryCount={}",
                getStreamKey(), record.recordId(), record.deliveryCount());

        try {
            StructuredDlqMessage dlqMessage = new StructuredDlqMessage(
                    getStreamKey(),
                    getGroupName(),
                    record.recordId(),
                    record.payload(),
                    record.deliveryCount(),
                    LocalDateTime.now(),
                    "Max retries (" + getMaxRetries() + ") exceeded."
            );

            String dlqKey = getStreamKey() + ":dlq";
            redisTemplate.opsForList().rightPush(dlqKey, dlqMessage.toJson(objectMapper));
            
            // DLQ 저장 성공 시 원본 Stream에서는 ACK 처리하여 제거
            ack(record.recordId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error(
                    "Poison Message를 DLQ에 저장하는 중 예외 발생. recordId={}", record.recordId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> findPendingRecordDeliveryCounts() {
        Object result = redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                "XPENDING",
                bytes(getStreamKey()),
                bytes(getGroupName()),
                bytes("IDLE"),
                bytes(String.valueOf(getMinIdleTime().toMillis())),
                bytes("-"),
                bytes("+"),
                bytes(String.valueOf(getClaimCount()))
        ));

        Map<String, Long> deliveryCounts = new HashMap<>();
        if (!(result instanceof List<?> pendingEntries)) {
            return deliveryCounts;
        }

        for (Object pendingEntry : pendingEntries) {
            if (pendingEntry instanceof List<?> fields && fields.size() >= 4) {
                String recordId = string(fields.get(0));
                long deliveryCount = Long.parseLong(string(fields.get(3)));
                deliveryCounts.put(recordId, deliveryCount);
            }
        }
        return deliveryCounts;
    }

    @SuppressWarnings("unchecked")
    private List<ClaimedRecord> claimPendingRecords(List<String> recordIds, Map<String, Long> deliveryCounts) {
        List<byte[]> args = new ArrayList<>();
        args.add(bytes(getStreamKey()));
        args.add(bytes(getGroupName()));
        args.add(bytes(recoveryConsumerName));
        args.add(bytes(String.valueOf(getMinIdleTime().toMillis())));
        recordIds.stream()
                .map(this::bytes)
                .forEach(args::add);

        Object result = redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                "XCLAIM",
                args.toArray(byte[][]::new)
        ));

        List<ClaimedRecord> claimedRecords = new ArrayList<>();
        if (!(result instanceof List<?> claimedEntries)) {
            return claimedRecords;
        }

        for (Object claimedEntry : claimedEntries) {
            if (!(claimedEntry instanceof List<?> entry) || entry.size() < 2) {
                continue;
            }

            String recordId = string(entry.get(0));
            Map<String, String> payload = parsePayload(entry.get(1));
            long deliveryCount = deliveryCounts.getOrDefault(recordId, 1L);

            claimedRecords.add(new ClaimedRecord(recordId, payload, deliveryCount));
        }
        return claimedRecords;
    }

    private Map<String, String> parsePayload(Object rawFields) {
        Map<String, String> payload = new HashMap<>();
        if (!(rawFields instanceof List<?> fields)) {
            return payload;
        }

        for (int i = 0; i + 1 < fields.size(); i += 2) {
            String key = string(fields.get(i));
            String value = string(fields.get(i + 1));
            payload.put(key, value);
        }
        return payload;
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String string(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    public record ClaimedRecord(String recordId, Map<String, String> payload, long deliveryCount) {
    }
}
