package com.team08.backend.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateDirectOrderRequest(
        @Schema(description = "강의 ID", example = "1")
        @NotNull Long courseId
) {
}
