package com.team08.backend.domain.couponissuerequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.coupon-issue-request.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueRequestPendingRecoveryScheduler {

    private static final Duration MIN_IDLE_TIME = Duration.ofMinutes(5);
    private static final int CLAIM_COUNT = 500;

    private final String recoveryConsumerName = "coupon-issue-request-recovery-" + UUID.randomUUID();

    private final StringRedisTemplate redisTemplate;
    private final CouponIssueRequestProcessor couponIssueRequestProcessor;

    @Scheduled(fixedDelay = 30_000)
    public void recoverPendingRequests() {
        List<String> pendingRecordIds = findPendingRecordIds();
        if (pendingRecordIds.isEmpty()) {
            return;
        }

        List<ClaimedRecord> claimedRecords = claimPendingRecords(pendingRecordIds);
        if (claimedRecords.isEmpty()) {
            return;
        }

        List<CouponIssueRequestProcessor.SelectedUserIssueCommand> commands = claimedRecords.stream()
                .map(ClaimedRecord::command)
                .toList();
        couponIssueRequestProcessor.processSelectedUsers(commands);
        redisTemplate.opsForStream().acknowledge(
                CouponIssueRequestStreamPublisher.STREAM_KEY,
                CouponIssueRequestStreamWorker.GROUP_NAME,
                claimedRecords.stream()
                        .map(ClaimedRecord::recordId)
                        .map(RecordId::of)
                        .toArray(RecordId[]::new)
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> findPendingRecordIds() {
        Object result = redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                "XPENDING",
                bytes(CouponIssueRequestStreamPublisher.STREAM_KEY),
                bytes(CouponIssueRequestStreamWorker.GROUP_NAME),
                bytes("IDLE"),
                bytes(String.valueOf(MIN_IDLE_TIME.toMillis())),
                bytes("-"),
                bytes("+"),
                bytes(String.valueOf(CLAIM_COUNT))
        ));
        if (!(result instanceof List<?> pendingEntries)) {
            return List.of();
        }

        List<String> recordIds = new ArrayList<>();
        for (Object pendingEntry : pendingEntries) {
            if (pendingEntry instanceof List<?> fields && !fields.isEmpty()) {
                recordIds.add(string(fields.get(0)));
            }
        }
        return recordIds;
    }

    @SuppressWarnings("unchecked")
    private List<ClaimedRecord> claimPendingRecords(List<String> recordIds) {
        List<byte[]> args = new ArrayList<>();
        args.add(bytes(CouponIssueRequestStreamPublisher.STREAM_KEY));
        args.add(bytes(CouponIssueRequestStreamWorker.GROUP_NAME));
        args.add(bytes(recoveryConsumerName));
        args.add(bytes(String.valueOf(MIN_IDLE_TIME.toMillis())));
        recordIds.stream()
                .map(this::bytes)
                .forEach(args::add);

        Object result = redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                "XCLAIM",
                args.toArray(byte[][]::new)
        ));
        if (!(result instanceof List<?> claimedEntries)) {
            return List.of();
        }

        List<ClaimedRecord> claimedRecords = new ArrayList<>();
        for (Object claimedEntry : claimedEntries) {
            if (!(claimedEntry instanceof List<?> entry) || entry.size() < 2) {
                continue;
            }

            String recordId = string(entry.get(0));
            CouponIssueRequestProcessor.SelectedUserIssueCommand command = toCommand(entry.get(1));
            if (command != null) {
                claimedRecords.add(new ClaimedRecord(recordId, command));
            }
        }
        return claimedRecords;
    }

    private CouponIssueRequestProcessor.SelectedUserIssueCommand toCommand(Object rawFields) {
        if (!(rawFields instanceof List<?> fields)) {
            return null;
        }

        Long requestId = null;
        Long policyId = null;
        Long userId = null;
        String issueKey = null;

        for (int i = 0; i + 1 < fields.size(); i += 2) {
            String key = string(fields.get(i));
            String value = string(fields.get(i + 1));
            if ("requestId".equals(key)) {
                requestId = Long.valueOf(value);
            } else if ("policyId".equals(key)) {
                policyId = Long.valueOf(value);
            } else if ("userId".equals(key)) {
                userId = Long.valueOf(value);
            } else if ("issueKey".equals(key)) {
                issueKey = value;
            }
        }

        if (requestId == null || policyId == null || userId == null || issueKey == null) {
            return null;
        }
        return new CouponIssueRequestProcessor.SelectedUserIssueCommand(requestId, policyId, userId, issueKey);
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

    private record ClaimedRecord(
            String recordId,
            CouponIssueRequestProcessor.SelectedUserIssueCommand command
    ) {
    }
}
