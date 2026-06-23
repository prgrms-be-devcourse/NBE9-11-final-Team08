package com.team08.backend.domain.payment.dto.toss;

public record TossConfirmPaymentRequest(
        String paymentKey,
        String orderId,
        int amount
) {
}
