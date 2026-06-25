package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicyCategory;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicyCourse;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyResponse(
        Long id,
        String name,
        CouponTarget couponTarget,
        CouponType couponType,
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
    public static CouponPolicyResponse from(CouponPolicy policy) {
        return new CouponPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getCouponTarget(),
                policy.getCouponType(),
                policy.getTotalQuantity(),
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getDiscountType(),
                policy.getDiscountValue(),
                policy.getMaxDiscountAmount(),
                policy.getMinOrderAmount(),
                policy.getValidDays(),
                policy.getIssueStartDate(),
                policy.getIssueEndDate(),
                policy.getTargetCategories().stream().map(CouponPolicyCategory::getCategoryId).toList(),
                policy.getTargetCourses().stream().map(CouponPolicyCourse::getCourseId).toList()
        );
    }
}
