package com.team08.backend.domain.couponpolicy.command;

import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponPolicyCreateCommand(
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
