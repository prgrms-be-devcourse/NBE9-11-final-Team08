package com.team08.backend.domain.order.dto;

import jakarta.validation.constraints.NotNull;

public record DirectOrderRequest(
        @NotNull Long courseId
) {
}