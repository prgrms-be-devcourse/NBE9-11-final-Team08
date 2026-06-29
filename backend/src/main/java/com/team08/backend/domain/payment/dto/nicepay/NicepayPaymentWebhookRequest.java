package com.team08.backend.domain.payment.dto.nicepay;

import java.time.OffsetDateTime;

public record NicepayPaymentWebhookRequest(
        String eventType,
        OffsetDateTime createdAt,
        NicepayPaymentWebhookData data,
        String paymentKey,
        String tid,
        String orderId,
        String status,
        Long amount
) {
    public String resolvedPaymentKey() {
        if (data != null && data.paymentKey() != null) {
            return data.paymentKey();
        }
        if (paymentKey != null) {
            return paymentKey;
        }
        return tid;
    }

    public String resolvedOrderId() {
        return data != null && data.orderId() != null ? data.orderId() : orderId;
    }

    public record NicepayPaymentWebhookData(
            String paymentKey,
            String tid,
            String orderId,
            String status,
            Long amount
    ) {
        public String paymentKey() {
            return paymentKey != null ? paymentKey : tid;
        }
    }
}
