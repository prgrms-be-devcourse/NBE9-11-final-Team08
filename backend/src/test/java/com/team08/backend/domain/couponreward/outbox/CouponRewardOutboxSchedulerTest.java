package com.team08.backend.domain.couponreward.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponRewardOutboxSchedulerTest {

    @Mock
    private CouponRewardOutboxEventRepository couponRewardOutboxEventRepository;

    @Mock
    private CouponRewardOutboxWorker couponRewardOutboxWorker;

    @Test
    @DisplayName("Sweeper는 재시도 가능한 outbox id를 조회해 각각 처리한다")
    void sweepPending_processesRetryableOutboxEventIds() {
        // given
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        CouponRewardOutboxProperties properties = new CouponRewardOutboxProperties(100, 5, 10, 600);
        CouponRewardOutboxScheduler scheduler = new CouponRewardOutboxScheduler(
                couponRewardOutboxEventRepository,
                couponRewardOutboxWorker,
                clock,
                properties
        );
        LocalDateTime now = LocalDateTime.now(clock);
        given(couponRewardOutboxEventRepository.findRetryableIds(
                CouponRewardOutboxEventStatus.PENDING.name(),
                CouponRewardOutboxEventStatus.FAILED.name(),
                now,
                100
        )).willReturn(List.of(10L, 11L));

        // when
        scheduler.sweepPending();

        // then
        then(couponRewardOutboxWorker).should().processOne(10L);
        then(couponRewardOutboxWorker).should().processOne(11L);
    }
}
