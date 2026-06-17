package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyUpdateRequest(
        @NotBlank String name,
        Integer totalQuantity,
        @NotNull CouponUsageType usageType,
        @NotNull Boolean isStackable,
        @NotNull DiscountType discountType,
        @NotNull Integer discountValue,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        Integer validDays,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate,
        List<Long> categoryIds,
        List<Long> courseIds
) {
}
