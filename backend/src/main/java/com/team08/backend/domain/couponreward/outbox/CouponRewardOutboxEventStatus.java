package com.team08.backend.domain.couponreward.outbox;

public enum CouponRewardOutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED,
    DEAD
}
