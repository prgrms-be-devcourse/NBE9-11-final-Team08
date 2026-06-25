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
        Long issuedCouponId,
        @Schema(description = "멱등 결제 요청 키. 같은 주문에 같은 키로 재요청하면 기존 결제 시도 결과를 반환합니다.", example = "pay-req-20260624-001")
        String idempotencyKey
) {
    public ConfirmPaymentRequest(String paymentKey, String method, int amount, Long issuedCouponId) {
        this(paymentKey, method, amount, issuedCouponId, null);
    }
}
