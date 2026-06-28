package com.team08.backend.domain.payment.provider;

import java.time.OffsetDateTime;

public record PaymentProviderConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        long totalAmount,
        OffsetDateTime approvedAt
) {
}
