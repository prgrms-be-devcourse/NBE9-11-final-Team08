package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        @Schema(description = "주문 ID", example = "1")
        Long orderId,
        @Schema(description = "주문 번호", example = "ORD-20260612101010-ABC12345")
        String orderNumber,
        @Schema(description = "총 주문 금액", example = "50000")
        int totalPrice,
        @Schema(description = "총 할인 금액", example = "0")
        int discountPrice,
        @Schema(description = "최종 결제 예정 금액", example = "50000")
        int finalPrice,
        @Schema(description = "주문 상태", example = "PENDING_PAYMENT")
        OrderStatus status,
        @Schema(description = "주문 일시")
        LocalDateTime orderedAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getDiscountPrice(),
                order.getFinalPrice(),
                order.getStatus(),
                order.getOrderedAt()
        );
    }
}
