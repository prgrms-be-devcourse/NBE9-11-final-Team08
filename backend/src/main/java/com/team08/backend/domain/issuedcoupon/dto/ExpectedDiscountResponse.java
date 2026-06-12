package com.team08.backend.domain.issuedcoupon.dto;

public record ExpectedDiscountResponse(
        String couponName,
        int originalPrice,
        int discountAmount,
        int finalPrice
) {
}
