package com.team08.backend.domain.coupon.dto;

import lombok.Builder;

@Builder
public record CouponResponse(
        String message
) {
    public static CouponResponse success(String message) {
        return new CouponResponse(message);
    }
}
