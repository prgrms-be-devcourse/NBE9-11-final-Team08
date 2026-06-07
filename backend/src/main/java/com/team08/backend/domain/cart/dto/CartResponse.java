package com.team08.backend.domain.cart.dto;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        Integer totalPrice
) {

    public static CartResponse from(List<CartItemResponse> items) {
        Integer totalPrice = items.stream()
                .mapToInt(CartItemResponse::price)
                .sum();

        return new CartResponse(items, totalPrice);
    }
}
