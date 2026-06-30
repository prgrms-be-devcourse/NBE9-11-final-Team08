package com.team08.backend.domain.couponissuerequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.global.redis.stream.AbstractPendingRecoveryScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.coupon-issue-request.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueRequestPendingRecoveryScheduler extends AbstractPendingRecoveryScheduler {

    private final CouponIssueRequestProcessor couponIssueRequestProcessor;

    public CouponIssueRequestPendingRecoveryScheduler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CouponIssueRequestProcessor couponIssueRequestProcessor) {
        super(redisTemplate, objectMapper, "coupon-issue-request");
        this.couponIssueRequestProcessor = couponIssueRequestProcessor;
    }

    @Override
    protected String getStreamKey() {
        return CouponIssueRequestStreamPublisher.STREAM_KEY;
    }

    @Override
    protected String getGroupName() {
        return CouponIssueRequestStreamWorker.GROUP_NAME;
    }

    @Scheduled(fixedDelay = 30_000)
    public void recover() {
        super.recoverPendingRecords();
    }

    @Override
    protected void processRetryRecords(List<ClaimedRecord> records, Consumer<String> ackCallback) {
        List<CouponIssueRequestProcessor.SelectedUserIssueCommand> commands = records.stream()
                .map(this::toCommand)
                .filter(java.util.Objects::nonNull)
                .toList();

        couponIssueRequestProcessor.processSelectedUsers(commands);
        records.forEach(record -> ackCallback.accept(record.recordId()));
    }

    private CouponIssueRequestProcessor.SelectedUserIssueCommand toCommand(ClaimedRecord record) {
        Map<String, String> payload = record.payload();
        try {
            Long requestId = Long.valueOf(payload.get("requestId"));
            Long policyId = Long.valueOf(payload.get("policyId"));
            Long userId = Long.valueOf(payload.get("userId"));
            String issueKey = payload.get("issueKey");

            return new CouponIssueRequestProcessor.SelectedUserIssueCommand(requestId, policyId, userId, issueKey);
        } catch (Exception e) {
            return null;
        }
    }
}
