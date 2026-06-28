package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;

import java.time.LocalDateTime;

public record CouponPolicySummaryResponse(
        Long id,
        String name,
        CouponTarget couponTarget,
        CouponType couponType,
        AutoIssueType autoIssueType,
        Integer totalQuantity,
        CouponUsageType usageType,
        Boolean isStackable,
        DiscountType discountType,
        Integer discountValue,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        Integer validDays,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate
) {
    public static CouponPolicySummaryResponse from(CouponPolicy policy) {
        return new CouponPolicySummaryResponse(
                policy.getId(),
                policy.getName(),
                policy.getCouponTarget(),
                policy.getCouponType(),
                policy.getAutoIssueType(),
                policy.getTotalQuantity(),
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getDiscountType(),
                policy.getDiscountValue(),
                policy.getMaxDiscountAmount(),
                policy.getMinOrderAmount(),
                policy.getValidDays(),
                policy.getIssueStartDate(),
                policy.getIssueEndDate()
        );
    }
}
