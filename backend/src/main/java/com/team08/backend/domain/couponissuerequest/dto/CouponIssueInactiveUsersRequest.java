package com.team08.backend.domain.couponissuerequest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponIssueInactiveUsersRequest(
        @NotBlank
        @Size(max = 80)
        String requestKey,

        @Min(1)
        int inactiveDays,

        @Min(1)
        Integer maxInactiveDays
) {
}
