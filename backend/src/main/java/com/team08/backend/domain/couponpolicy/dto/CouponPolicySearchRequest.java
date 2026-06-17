package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponType;

public record CouponPolicySearchRequest(
        String name,
        CouponType couponType,
        CouponStatus status
) {
}
