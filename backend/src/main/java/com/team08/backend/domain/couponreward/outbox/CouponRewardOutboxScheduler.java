package com.team08.backend.domain.couponreward.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.coupon-reward.outbox.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CouponRewardOutboxScheduler {

    private final CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;
    private final CouponRewardOutboxWorker couponRewardOutboxWorker;
    private final Clock clock;
    private final CouponRewardOutboxProperties couponRewardOutboxProperties;

    @Scheduled(fixedDelayString = "${app.coupon-reward.outbox.sweeper-delay-ms:300000}")
    public void sweepPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> eventIds = couponRewardOutboxEventRepository.findRetryableIds(
                CouponRewardOutboxEventStatus.PENDING.name(),
                CouponRewardOutboxEventStatus.FAILED.name(),
                now,
                couponRewardOutboxProperties.batchSize()
        );

        for (Long eventId : eventIds) {
            couponRewardOutboxWorker.processOne(eventId);
        }
    }
}
