package com.team08.backend.domain.issuedcoupon.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;

import java.time.LocalDateTime;

public record CouponListResponse(
        Long issuedCouponId,
        String couponName,
        Integer discountValue,
        DiscountType discountType,
        LocalDateTime expiredAt,
        CouponStatus status,
        CouponUsageType usageType,
        Boolean isStackable
) {
    public static CouponListResponse of(IssuedCoupon coupon, CouponPolicy policy) {
        return new CouponListResponse(
                coupon.getId(),
                policy.getName(),
                policy.getDiscountValue(),
                policy.getDiscountType(),
                coupon.getExpiredAt(),
                coupon.getStatus(),
                policy.getUsageType(),
                policy.getIsStackable()
        );
    }
}
