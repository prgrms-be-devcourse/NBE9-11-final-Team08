package com.team08.backend.domain.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long courseId
) {
}
