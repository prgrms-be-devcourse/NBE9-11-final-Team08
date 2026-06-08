package com.team08.backend.domain.payment.dto;

public record MockPaymentSuccessRequest(
        String paymentKey,
        String method
) {
}
