package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicycategory.entity.CouponPolicyCategory;
import com.team08.backend.domain.couponpolicycourse.entity.CouponPolicyCourse;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyDetailResponse(
        Long id,
        String name,
        DiscountType discountType,
        Integer discountValue,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        Integer validDays,
        Integer totalQuantity,
        List<Long> categoryIds,
        List<Long> courseIds,
        CouponType couponType,
        CouponTarget couponTarget,
        CouponUsageType usageType,
        Boolean isStackable,
        LocalDateTime issueStartDate,
        LocalDateTime issueEndDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CouponPolicyDetailResponse from(CouponPolicy policy) {
        return new CouponPolicyDetailResponse(
                policy.getId(),
                policy.getName(),
                policy.getDiscountType(),
                policy.getDiscountValue(),
                policy.getMaxDiscountAmount(),
                policy.getMinOrderAmount(),
                policy.getValidDays(),
                policy.getTotalQuantity(),
                policy.getTargetCategories().stream()
                        .map(CouponPolicyCategory::getCategoryId)
                        .toList(),
                policy.getTargetCourses().stream()
                        .map(CouponPolicyCourse::getCourseId)
                        .toList(),
                policy.getCouponType(),
                policy.getCouponTarget(),
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getIssueStartDate(),
                policy.getIssueEndDate(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
