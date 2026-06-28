package com.team08.backend.domain.payment.provider;

public record PaymentProviderConfirmRequest(
        String paymentKey,
        String orderId,
        int amount
) {
}
