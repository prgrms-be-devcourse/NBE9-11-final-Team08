package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;

import com.team08.backend.domain.couponpolicycategory.entity.CouponPolicyCategory;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyResponse(
        Long id,
        String name,
        DiscountType discountType,
        Integer discountValue,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        Integer validDays,
        Integer totalQuantity,
        List<Long> categoryIds,
        CouponType couponType,
        CouponTarget couponTarget,
        CouponUsageType usageType,
        Boolean isStackable,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate
) {
    public static CouponPolicyResponse from(CouponPolicy policy) {
        return new CouponPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getDiscountType(),
                policy.getDiscountValue(),
                policy.getMaxDiscountAmount(),
                policy.getMinOrderAmount(),
                policy.getValidDays(),
                policy.getTotalQuantity(),
                policy.getTargetCategories().stream().map(CouponPolicyCategory::getCategoryId).toList(),
                policy.getCouponType(),
                policy.getCouponTarget(),
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getIssueStartDate(),
                policy.getIssueEndDate()
        );
    }
}
