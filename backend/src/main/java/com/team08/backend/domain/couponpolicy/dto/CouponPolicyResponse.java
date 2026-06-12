package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponPolicyResponse(
        Long id,
        String name,
        DiscountType discountType,
        Integer discountValue,
        Integer totalQuantity,
        CouponType couponType,
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
                policy.getTotalQuantity(),
                policy.getCouponType(),
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getIssueStartDate(),
                policy.getIssueEndDate()
        );
    }
}
