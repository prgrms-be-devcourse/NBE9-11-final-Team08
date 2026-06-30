package com.team08.backend.domain.issuedcoupon.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicyCategory;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicyCourse;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;

import java.time.LocalDateTime;
import java.util.List;

public record CouponListResponse(
        Long issuedCouponId,
        Long policyId,
        String couponName,
        Integer discountValue,
        DiscountType discountType,
        LocalDateTime expiredAt,
        CouponStatus status,
        CouponUsageType usageType,
        Boolean isStackable,
        Integer maxDiscountAmount,
        Integer minOrderAmount,
        CouponTarget couponTarget,
        List<Long> categoryIds,
        List<Long> courseIds
) {
    public static CouponListResponse of(IssuedCoupon coupon, CouponPolicy policy, LocalDateTime now) {
        // 지연 평가
        CouponStatus effectiveStatus = coupon.getStatus();
        if (effectiveStatus == CouponStatus.ISSUED && coupon.getExpiredAt().isBefore(now)) {
            effectiveStatus = CouponStatus.EXPIRED;
        }

        return new CouponListResponse(
                coupon.getId(),
                policy.getId(),
                policy.getName(),
                policy.getDiscountValue(),
                policy.getDiscountType(),
                coupon.getExpiredAt(),
                effectiveStatus,
                policy.getUsageType(),
                policy.getIsStackable(),
                policy.getMaxDiscountAmount(),
                policy.getMinOrderAmount(),
                policy.getCouponTarget(),
                policy.getTargetCategories().stream().map(CouponPolicyCategory::getCategoryId).toList(),
                policy.getTargetCourses().stream().map(CouponPolicyCourse::getCourseId).toList()
        );
    }
}
