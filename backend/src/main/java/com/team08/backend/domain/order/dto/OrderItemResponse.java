package com.team08.backend.domain.order.dto;

import com.team08.backend.domain.order.entity.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long courseId,
        String courseTitle,
        Integer price,
        Integer discountPrice,
        Integer finalPrice
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getCourse().getId(),
                orderItem.getCourseTitle(),
                orderItem.getPrice(),
                orderItem.getDiscountPrice(),
                orderItem.getFinalPrice()
        );
    }
}
