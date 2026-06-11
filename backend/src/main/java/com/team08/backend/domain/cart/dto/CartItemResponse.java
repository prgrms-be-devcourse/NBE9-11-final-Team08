package com.team08.backend.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CartItemResponse(
        @Schema(description = "장바구니 항목 ID", example = "1")
        Long cartItemId,
        @Schema(description = "강의 ID", example = "1")
        Long courseId,
        @Schema(description = "강의 제목", example = "Spring Boot 입문")
        String title,
        @Schema(description = "강의 가격", example = "30000")
        int price
) {
}
