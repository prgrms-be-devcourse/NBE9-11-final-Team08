package com.team08.backend.domain.couponissuerequest.service;

import com.team08.backend.global.redis.stream.AbstractScheduledBatchStreamWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.coupon-issue-request.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueRequestStreamWorker extends AbstractScheduledBatchStreamWorker {

    static final String GROUP_NAME = "coupon-issue-request-workers";
    static final int BATCH_SIZE = 1000;

    private final CouponIssueRequestProcessor couponIssueRequestProcessor;

    public CouponIssueRequestStreamWorker(StringRedisTemplate redisTemplate, CouponIssueRequestProcessor couponIssueRequestProcessor) {
        super(redisTemplate, "coupon-issue-request-worker");
        this.couponIssueRequestProcessor = couponIssueRequestProcessor;
    }

    @Override
    protected String getStreamKey() {
        return CouponIssueRequestStreamPublisher.STREAM_KEY;
    }

    @Override
    protected String getGroupName() {
        return GROUP_NAME;
    }

    @Override
    protected int getBatchSize() {
        return BATCH_SIZE;
    }

    @Scheduled(fixedDelay = 1000)
    public void processRequests() {
        super.pollAndProcess();
    }

    @Override
    protected void processBatch(List<MapRecord<String, Object, Object>> records, java.util.function.Consumer<MapRecord<String, Object, Object>> ackCallback) {
        List<CouponIssueRequestProcessor.SelectedUserIssueCommand> commands = records.stream()
                .map(this::toCommand)
                .toList();
        
        couponIssueRequestProcessor.processSelectedUsers(commands);
        records.forEach(ackCallback);
    }

    private CouponIssueRequestProcessor.SelectedUserIssueCommand toCommand(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        Long requestId = Long.valueOf(String.valueOf(value.get("requestId")));
        Long policyId = Long.valueOf(String.valueOf(value.get("policyId")));
        Long userId = Long.valueOf(String.valueOf(value.get("userId")));
        String issueKey = String.valueOf(value.get("issueKey"));

        return new CouponIssueRequestProcessor.SelectedUserIssueCommand(requestId, policyId, userId, issueKey);
    }
}
