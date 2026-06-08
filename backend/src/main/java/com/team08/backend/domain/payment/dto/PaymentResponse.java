package com.team08.backend.domain.payment.dto;

import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        String paymentKey,
        String method,
        Integer amount,
        PaymentStatus status,
        LocalDateTime paidAt,
        String failedReason
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentKey(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getFailedReason()
        );
    }
}
