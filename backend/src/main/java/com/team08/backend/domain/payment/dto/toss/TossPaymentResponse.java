package com.team08.backend.domain.payment.dto.toss;

import java.time.OffsetDateTime;

public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        TossEasyPayResponse easyPay,
        long totalAmount,
        OffsetDateTime approvedAt
) {

    public TossPaymentResponse(
            String paymentKey,
            String orderId,
            String status,
            String method,
            long totalAmount,
            OffsetDateTime approvedAt
    ) {
        this(paymentKey, orderId, status, method, null, totalAmount, approvedAt);
    }

    public String resolvedMethod() {
        if (easyPay != null && easyPay.provider() != null && !easyPay.provider().isBlank()) {
            return easyPay.provider();
        }
        return method;
    }

    public record TossEasyPayResponse(String provider) {
    }
}
