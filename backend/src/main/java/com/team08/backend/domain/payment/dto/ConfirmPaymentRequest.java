package com.team08.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmPaymentRequest(
        @Schema(description = "PG 결제 키", example = "payment-key")
        String paymentKey,
        @Schema(description = "결제 수단", example = "CARD")
        String method,
        @Schema(description = "결제 승인 요청 금액", example = "30000")
        int amount,
        @Schema(description = "적용할 발급 쿠폰 ID", example = "1")
        Long issuedCouponId
) {
}
