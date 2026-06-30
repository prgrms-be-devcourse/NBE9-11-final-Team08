package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
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
        LocalDateTime orderedAt,
        @Schema(description = "주문 취소 일시")
        LocalDateTime canceledAt,
        @Schema(description = "결제 요약 정보")
        OrderPaymentResponse payment,
        @Schema(description = "주문 항목 목록")
        List<OrderItemResponse> items
) {
    public static OrderDetailResponse from(Order order, List<OrderItem> orderItems) {
        return from(order, orderItems, null);
    }

    public static OrderDetailResponse from(Order order, List<OrderItem> orderItems, OrderPaymentResponse payment) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getDiscountPrice(),
                order.getFinalPrice(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getCanceledAt(),
                payment,
                orderItems.stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
