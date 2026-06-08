package com.team08.backend.domain.coupon.dto;

import com.team08.backend.domain.coupon.entity.CouponStatus;
import com.team08.backend.domain.coupon.entity.DiscountType;
import com.team08.backend.domain.coupon.entity.IssuedCoupon;

import java.time.LocalDateTime;

public record CouponListResponse(
        Long issuedCouponId,
        String couponName,
        Integer discountValue,
        DiscountType discountType,
        LocalDateTime expiredAt,
        CouponStatus status
) {
    public static CouponListResponse from(IssuedCoupon coupon) {
        return new CouponListResponse(
                coupon.getId(),
                coupon.getPolicy().getName(),
                coupon.getPolicy().getDiscountValue(),
                coupon.getPolicy().getDiscountType(),
                coupon.getExpiredAt(),
                coupon.getStatus()
        );
    }
}
