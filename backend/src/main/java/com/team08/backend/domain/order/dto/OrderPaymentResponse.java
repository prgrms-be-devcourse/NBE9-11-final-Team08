package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OrderPaymentResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,
        @Schema(description = "결제 Provider", example = "TOSS")
        PaymentProviderType provider,
        @Schema(description = "결제 수단", example = "CARD")
        String method,
        @Schema(description = "결제 상태", example = "SUCCESS")
        PaymentStatus status,
        @Schema(description = "결제 완료 일시")
        LocalDateTime paidAt
) {
    public static OrderPaymentResponse from(Payment payment) {
        return new OrderPaymentResponse(
                payment.getId(),
                payment.getProvider(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
