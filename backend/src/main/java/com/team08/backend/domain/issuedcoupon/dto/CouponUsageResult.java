package com.team08.backend.domain.issuedcoupon.dto;

import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import java.util.List;

public record CouponUsageResult(
        int totalDiscount,
        List<OrderCouponUsage> usages
) {
}
