package com.team08.backend.domain.payment.dto.nicepay;

public record NicepayConfirmPaymentRequest(
        String paymentKey,
        String orderId,
        int amount
) {
}
