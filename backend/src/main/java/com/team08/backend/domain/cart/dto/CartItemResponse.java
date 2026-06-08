package com.team08.backend.domain.cart.dto;

import com.team08.backend.domain.cart.entity.CartItem;

import java.time.LocalDateTime;

public record CartItemResponse(
        Long cartItemId,
        Long courseId,
        String courseTitle,
        Integer price,
        LocalDateTime createdAt
) {

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getCourse().getId(),
                cartItem.getCourse().getTitle(),
                cartItem.getPrice(),
                cartItem.getCreatedAt()
        );
    }
}
