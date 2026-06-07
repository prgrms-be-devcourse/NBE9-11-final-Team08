package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String orderNumber,
        Integer totalPrice,
        Integer discountPrice,
        Integer finalPrice,
        OrderStatus status,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        List<OrderItemResponse> items
) {

    public static OrderDetailResponse of(Order order, List<OrderItemResponse> items) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalPrice(),
                order.getDiscountPrice(),
                order.getFinalPrice(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getPaidAt(),
                order.getCanceledAt(),
                items
        );
    }
}
