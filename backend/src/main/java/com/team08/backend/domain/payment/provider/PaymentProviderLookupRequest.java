package com.team08.backend.domain.payment.provider;

public record PaymentProviderLookupRequest(
        String paymentKey,
        String orderId
) {
}
