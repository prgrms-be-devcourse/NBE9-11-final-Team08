package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyUpdateRequest(
        @NotBlank String name,
        @NotNull DiscountType discountType,
        @NotNull Integer discountValue,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        Integer validDays,
        Integer totalQuantity,
        List<Long> categoryIds,
        List<Long> courseIds,
        @NotNull CouponTarget couponTarget,
        Boolean isStackable,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate
) implements CouponPolicyValidatable {
}
