package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.*;

import java.time.LocalDateTime;

public record CouponPolicyCreateRequest(
        String name,
        DiscountType discountType,
        Integer discountValue,
        Integer validDays,
        Integer totalQuantity,
        Long categoryId,
        CouponType couponType,
        CouponTarget couponTarget,
        CouponUsageType usageType,
        Boolean isStackable,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate
) {
}
