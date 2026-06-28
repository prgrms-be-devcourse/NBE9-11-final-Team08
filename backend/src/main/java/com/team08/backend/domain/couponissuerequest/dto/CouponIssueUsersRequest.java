package com.team08.backend.domain.couponissuerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CouponIssueUsersRequest(
        @NotBlank
        @Size(max = 80)
        String requestKey,

        @NotEmpty
        List<Long> userIds
) {
}
