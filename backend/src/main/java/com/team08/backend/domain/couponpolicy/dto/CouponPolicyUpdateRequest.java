package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyUpdateRequest(
        @NotBlank String name,
        @Min(value = 1, message = "쿠폰 수량은 1 이상이어야 합니다.")
        Integer totalQuantity,
        @NotNull CouponUsageType usageType,
        @NotNull Boolean isStackable,
        @NotNull DiscountType discountType,
        @NotNull @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
        Integer discountValue,
        @Min(value = 1, message = "최대 할인 금액은 1 이상이어야 합니다.")
        Integer maxDiscountAmount,
        @Min(value = 1, message = "최소 주문 금액은 1 이상이어야 합니다.")
        Integer minOrderAmount,
        @Min(value = 0, message = "쿠폰 유효 기간은 0 이상이어야 합니다.")
        Integer validDays,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate,
        List<Long> categoryIds,
        List<Long> courseIds
) {
}
