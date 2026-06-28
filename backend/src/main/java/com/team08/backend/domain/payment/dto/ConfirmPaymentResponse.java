package com.team08.backend.domain.payment.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ConfirmPaymentResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,
        @Schema(description = "주문 ID", example = "1")
        Long orderId,
        @Schema(description = "주문 번호", example = "ORD-20260612101010-ABC12345")
        String orderNumber,
        @Schema(description = "결제 금액", example = "50000")
        int amount,
        @Schema(description = "결제 상태", example = "SUCCESS")
        PaymentStatus paymentStatus,
        @Schema(description = "주문 상태", example = "PAID")
        OrderStatus orderStatus,
        @Schema(description = "결제 완료 일시")
        LocalDateTime paidAt,
        @Schema(description = "수강권 발급은 Outbox 후처리에서 비동기로 수행되므로 confirm 응답에서는 빈 목록")
        List<Long> enrolledCourseIds
) {
    public static ConfirmPaymentResponse from(Payment payment, Order order) {
        return new ConfirmPaymentResponse(
                payment.getId(),
                order.getId(),
                order.getOrderNumber(),
                payment.getAmount(),
                payment.getStatus(),
                order.getStatus(),
                payment.getPaidAt(),
                List.of()
        );
    }
}
