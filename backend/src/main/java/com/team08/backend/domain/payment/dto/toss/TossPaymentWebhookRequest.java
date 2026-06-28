package com.team08.backend.domain.payment.dto.toss;

import java.time.OffsetDateTime;

public record TossPaymentWebhookRequest(
        String eventType,
        OffsetDateTime createdAt,
        TossPaymentWebhookData data
) {
    public record TossPaymentWebhookData(
            String paymentKey,
            String orderId,
            String status,
            Long totalAmount
    ) {
    }
}
