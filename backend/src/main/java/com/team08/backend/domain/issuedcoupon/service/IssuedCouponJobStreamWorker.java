package com.team08.backend.domain.issuedcoupon.service;

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
    protected void processBatch(List<MapRecord<String, Object, Object>> records, java.util.function.Consumer<MapRecord<String, Object, Object>> ackCallback) throws Exception {
        for (MapRecord<String, Object, Object> record : records) {
            try {
                Map<Object, Object> value = record.getValue();
                Long jobId = Long.valueOf(String.valueOf(value.get("jobId")));
                issuedCouponJobProcessor.process(jobId);
                
                // 단건 처리 성공 시 즉시 ACK
                ackCallback.accept(record);
            } catch (Exception e) {
                // 특정 건 실패 시 예외만 기록하고 ACK하지 않음 (PEL에 대기)
                // 전체 배치가 중단되지 않도록 catch로 감싸서 다음 레코드 처리
                org.slf4j.LoggerFactory.getLogger(IssuedCouponJobStreamWorker.class)
                        .warn("IssuedCouponJobStreamWorker 처리 실패. recordId={}", record.getId(), e);
            }
        }
    }
}
