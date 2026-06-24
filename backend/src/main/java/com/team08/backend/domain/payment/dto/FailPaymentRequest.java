package com.team08.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FailPaymentRequest(
        @Schema(description = "PG 결제 키", example = "payment-key")
        String paymentKey,
        @Schema(description = "결제 수단", example = "CARD")
        String method,
        @Schema(description = "결제 실패 요청 금액", example = "30000")
        int amount,
        @Schema(description = "결제 실패 사유", example = "카드 승인 실패")
        String failedReason,
        @Schema(description = "적용 시도한 발급 쿠폰 ID", example = "1")
        Long issuedCouponId
) {
}
