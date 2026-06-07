package com.team08.backend.domain.coupon.dto;

import lombok.Builder;

@Builder
public record ExpectedDiscountResponse(
        String couponName,  // 쿠폰 이름
        int originalPrice,  // 원래 가격
        int discountAmount,  // 할인 가격
        int finalPrice  // 할인된 가격
) {
}
