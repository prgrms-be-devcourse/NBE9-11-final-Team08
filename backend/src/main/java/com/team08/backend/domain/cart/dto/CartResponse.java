package com.team08.backend.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CartResponse(
        @Schema(description = "장바구니 항목 목록")
        List<CartItemResponse> items,
        @Schema(description = "장바구니 총 금액", example = "30000")
        int totalPrice
) {
    public static CartResponse empty() {
        return new CartResponse(List.of(), 0);
    }
}
