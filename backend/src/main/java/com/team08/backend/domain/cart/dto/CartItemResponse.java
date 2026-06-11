package com.team08.backend.domain.cart.dto;

public record CartItemResponse(
        Long cartItemId,
        Long courseId,
        String title,
        int price
) {
}
