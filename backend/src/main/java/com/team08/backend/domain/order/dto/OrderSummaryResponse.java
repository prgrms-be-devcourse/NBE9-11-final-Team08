package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        String orderNumber,
        Integer totalPrice,
        Integer discountPrice,
        Integer finalPrice,
        OrderStatus status,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getDiscountPrice(),
                order.getFinalPrice(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getPaidAt(),
                order.getCanceledAt()
        );
    }
}
