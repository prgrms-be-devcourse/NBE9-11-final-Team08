package com.team08.backend.domain.couponreward.outbox.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.couponreward.outbox.service.CouponRewardOutboxWorker;
import com.team08.backend.global.redis.stream.AbstractPendingRecoveryScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.coupon-reward.stream-worker.enabled", havingValue = "true", matchIfMissing = true)
public class CouponRewardOutboxPendingRecoveryScheduler extends AbstractPendingRecoveryScheduler {

    private final CouponRewardOutboxWorker couponRewardOutboxWorker;

    public CouponRewardOutboxPendingRecoveryScheduler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CouponRewardOutboxWorker couponRewardOutboxWorker) {
        super(redisTemplate, objectMapper, "coupon-reward-outbox");
        this.couponRewardOutboxWorker = couponRewardOutboxWorker;
    }

    @Override
    protected String getStreamKey() {
        return CouponRewardOutboxStreamPublishListener.STREAM_KEY;
    }

    @Override
    protected String getGroupName() {
        return CouponRewardOutboxStreamWorker.GROUP_NAME;
    }

    @Scheduled(fixedDelay = 30_000)
    public void recover() {
        super.recoverPendingRecords();
    }

    @Override
    protected void processRetryRecords(List<ClaimedRecord> records, Consumer<String> ackCallback) throws Exception {
        for (ClaimedRecord record : records) {
            try {
                String payloadStr = record.payload().get("outboxEventId");
                if (payloadStr != null) {
                    couponRewardOutboxWorker.processOne(Long.valueOf(payloadStr));
                }
                ackCallback.accept(record.recordId());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(CouponRewardOutboxPendingRecoveryScheduler.class)
                        .warn("Reward Outbox 복구 처리 중 예외 발생. recordId={}", record.recordId(), e);
            }
        }
    }
}
