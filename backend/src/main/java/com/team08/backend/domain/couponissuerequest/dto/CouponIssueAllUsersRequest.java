package com.team08.backend.domain.couponissuerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponIssueAllUsersRequest(
        @NotBlank
        @Size(max = 80)
        String requestKey
) {
}
