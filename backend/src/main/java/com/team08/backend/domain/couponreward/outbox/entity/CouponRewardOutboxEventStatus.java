package com.team08.backend.domain.couponreward.outbox.entity;

public enum CouponRewardOutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED,
    DEAD
}
