package com.team08.backend.domain.payment.dto.toss;

public record TossPaymentErrorResponse(
        String code,
        String message
) {
}
