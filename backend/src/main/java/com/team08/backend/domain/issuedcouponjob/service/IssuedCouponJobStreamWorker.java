package com.team08.backend.domain.issuedcouponjob.service;

import com.team08.backend.global.redis.stream.AbstractScheduledBatchStreamWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.issued-coupon-job.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class IssuedCouponJobStreamWorker extends AbstractScheduledBatchStreamWorker {

    private static final String GROUP_NAME = "coupon-issue-workers";
    private static final int BATCH_SIZE = 1000;

    private final IssuedCouponJobProcessor issuedCouponJobProcessor;

    public IssuedCouponJobStreamWorker(StringRedisTemplate redisTemplate, IssuedCouponJobProcessor issuedCouponJobProcessor) {
        super(redisTemplate, "coupon-issue-worker");
        this.issuedCouponJobProcessor = issuedCouponJobProcessor;
    }

    @Override
    protected String getStreamKey() {
        return IssuedCouponJobStreamPublisher.STREAM_KEY;
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
    public void processJobs() {
        super.pollAndProcess();
    }

    @Override
    protected void processBatch(List<MapRecord<String, Object, Object>> records, java.util.function.Consumer<MapRecord<String, Object, Object>> ackCallback) {
        for (MapRecord<String, Object, Object> record : records) {
            try {
                Map<Object, Object> value = record.getValue();
                String requestId = String.valueOf(value.get("requestId"));
                Long userId = Long.valueOf(String.valueOf(value.get("userId")));
                Long policyId = Long.valueOf(String.valueOf(value.get("policyId")));
                
                issuedCouponJobProcessor.process(requestId, userId, policyId);
                
                ackCallback.accept(record);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(IssuedCouponJobStreamWorker.class)
                        .warn("IssuedCouponJobStreamWorker 처리 실패. recordId={}", record.getId(), e);
            }
        }
    }
}
