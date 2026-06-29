package com.team08.backend.domain.issuedcoupon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.global.redis.stream.AbstractPendingRecoveryScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.issued-coupon-job.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class IssuedCouponJobPendingRecoveryScheduler extends AbstractPendingRecoveryScheduler {

    private final IssuedCouponJobProcessor issuedCouponJobProcessor;

    public IssuedCouponJobPendingRecoveryScheduler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            IssuedCouponJobProcessor issuedCouponJobProcessor) {
        super(redisTemplate, objectMapper, "issued-coupon-job");
        this.issuedCouponJobProcessor = issuedCouponJobProcessor;
    }

    @Override
    protected String getStreamKey() {
        return IssuedCouponJobStreamPublisher.STREAM_KEY;
    }

    @Override
    protected String getGroupName() {
        return "coupon-issue-workers";
    }

    @Scheduled(fixedDelay = 30_000)
    public void recover() {
        super.recoverPendingRecords();
    }

    @Override
    protected void processRetryRecords(List<ClaimedRecord> records, Consumer<String> ackCallback) throws Exception {
        for (ClaimedRecord record : records) {
            try {
                String jobIdStr = record.payload().get("jobId");
                if (jobIdStr != null) {
                    Long jobId = Long.valueOf(jobIdStr);
                    issuedCouponJobProcessor.process(jobId);
                }
                ackCallback.accept(record.recordId());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(IssuedCouponJobPendingRecoveryScheduler.class)
                        .warn("Issued Coupon Job 복구 처리 중 예외 발생. recordId={}", record.recordId(), e);
            }
        }
    }
}
