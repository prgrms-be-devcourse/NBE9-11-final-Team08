package com.team08.backend.domain.payment.dto.toss;

import java.time.OffsetDateTime;

public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        int totalAmount,
        OffsetDateTime approvedAt
) {
}
