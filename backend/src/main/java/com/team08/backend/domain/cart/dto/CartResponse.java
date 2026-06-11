package com.team08.backend.domain.cart.dto;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int totalPrice
) {
    public static CartResponse empty() {
        return new CartResponse(List.of(), 0);
    }
}
