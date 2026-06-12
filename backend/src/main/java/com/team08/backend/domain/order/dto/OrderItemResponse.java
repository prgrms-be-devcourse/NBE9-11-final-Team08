package com.team08.backend.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderItemResponse(
        @Schema(description = "주문 항목 ID", example = "1")
        Long orderItemId,
        @Schema(description = "강의 ID", example = "1")
        Long courseId,
        @Schema(description = "주문 시점 강의 제목", example = "Spring Boot 입문")
        String courseTitle,
        @Schema(description = "정가", example = "30000")
        int price,
        @Schema(description = "할인 금액", example = "0")
        int discountPrice,
        @Schema(description = "최종 결제 예정 금액", example = "30000")
        int finalPrice
) {
}
