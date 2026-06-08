package com.team08.backend.domain.coupon.dto;

import com.team08.backend.domain.coupon.entity.CouponTarget;
import com.team08.backend.domain.coupon.entity.CouponType;
import com.team08.backend.domain.coupon.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponPolicyCreateRequest(
        String name,
        DiscountType discountType,
        Integer discountValue,
        Integer validDays,
        Integer totalQuantity,
        CouponType couponType,
        CouponTarget couponTarget,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate
) {
}
