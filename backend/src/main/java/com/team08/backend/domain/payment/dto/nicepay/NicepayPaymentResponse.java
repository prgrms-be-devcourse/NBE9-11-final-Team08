package com.team08.backend.domain.payment.dto.nicepay;

import java.time.OffsetDateTime;

public record NicepayPaymentResponse(
        String resultCode,
        String resultMsg,
        String paymentKey,
        String orderId,
        String status,
        String method,
        long amount,
        OffsetDateTime approvedAt
) {
}
