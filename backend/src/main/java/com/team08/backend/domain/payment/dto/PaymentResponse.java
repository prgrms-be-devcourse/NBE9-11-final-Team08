package com.team08.backend.domain.payment.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PaymentResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,
        @Schema(description = "주문 ID", example = "1")
        Long orderId,
        @Schema(description = "결제 금액", example = "30000")
        int amount,
        @Schema(description = "결제 상태", example = "FAILED")
        PaymentStatus paymentStatus,
        @Schema(description = "주문 상태", example = "PENDING_PAYMENT")
        OrderStatus orderStatus,
        @Schema(description = "결제 완료 일시")
        LocalDateTime paidAt,
        @Schema(description = "결제 실패 사유")
        String failedReason,
        @Schema(description = "결제 취소 일시")
        LocalDateTime canceledAt,
        @Schema(description = "환불 일시")
        LocalDateTime refundedAt
) {
    public static PaymentResponse from(Payment payment, Order order) {
        return new PaymentResponse(
                payment.getId(),
                order.getId(),
                payment.getAmount(),
                payment.getStatus(),
                order.getStatus(),
                payment.getPaidAt(),
                payment.getFailedReason(),
                payment.getCanceledAt(),
                payment.getRefundedAt()
        );
    }
}
