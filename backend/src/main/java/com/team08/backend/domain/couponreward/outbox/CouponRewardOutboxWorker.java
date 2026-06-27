package com.team08.backend.domain.couponreward.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRewardOutboxWorker {

    private final CouponRewardOutboxTransactionService couponRewardOutboxTransactionService;

    public void processOne(Long eventId) {
        try {
            couponRewardOutboxTransactionService.issueAndMarkProcessed(eventId);
        } catch (RuntimeException e) {
            log.warn("쿠폰 보상 outbox 처리 실패. eventId={}", eventId, e);
            couponRewardOutboxTransactionService.markFailed(eventId, e);
        }
    }
}
