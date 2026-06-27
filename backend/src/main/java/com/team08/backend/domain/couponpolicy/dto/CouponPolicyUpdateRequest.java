package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyUpdateRequest(
        @NotBlank String name,
        AutoIssueType autoIssueType,
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
    public CouponPolicyUpdateRequest(
            String name,
            Integer totalQuantity,
            CouponUsageType usageType,
            Boolean isStackable,
            DiscountType discountType,
            Integer discountValue,
            Integer maxDiscountAmount,
            Integer minOrderAmount,
            Integer validDays,
            LocalDateTime issueStartDate,
            LocalDateTime issueEndDate,
            List<Long> categoryIds,
            List<Long> courseIds
    ) {
        this(
                name,
                null,
                totalQuantity,
                usageType,
                isStackable,
                discountType,
                discountValue,
                maxDiscountAmount,
                minOrderAmount,
                validDays,
                issueStartDate,
                issueEndDate,
                categoryIds,
                courseIds
        );
    }
}
