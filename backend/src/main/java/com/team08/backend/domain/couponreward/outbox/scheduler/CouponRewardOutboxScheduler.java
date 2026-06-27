package com.team08.backend.domain.couponreward.outbox.scheduler;

import com.team08.backend.domain.couponreward.outbox.entity.CouponRewardOutboxEventStatus;
import com.team08.backend.domain.couponreward.outbox.repository.CouponRewardOutboxEventRepository;
import com.team08.backend.domain.couponreward.outbox.service.CouponRewardOutboxWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponRewardOutboxScheduler {

    private static final int BATCH_SIZE = 100;
    private static final long SWEEPER_DELAY_MS = 300000;

    private final CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;
    private final CouponRewardOutboxWorker couponRewardOutboxWorker;
    private final Clock clock;

    @Scheduled(fixedDelay = SWEEPER_DELAY_MS)
    public void sweepPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> eventIds = couponRewardOutboxEventRepository.findRetryableIds(
                CouponRewardOutboxEventStatus.PENDING.name(),
                CouponRewardOutboxEventStatus.FAILED.name(),
                now,
                BATCH_SIZE
        );

        for (Long eventId : eventIds) {
            couponRewardOutboxWorker.processOne(eventId);
        }
    }
}
