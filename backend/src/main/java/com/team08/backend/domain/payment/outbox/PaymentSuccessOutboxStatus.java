package com.team08.backend.domain.payment.outbox;

public enum PaymentSuccessOutboxStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DEAD
}
