package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponPolicyValidatable {
    String name();
    DiscountType discountType();
    Integer discountValue();
    Integer maxDiscountAmount();
    Integer minOrderAmount();
    Integer validDays();
    Integer totalQuantity();
    Long categoryId();
    List<Long> courseIds();
    CouponTarget couponTarget();
    LocalDateTime issueStartDate();
    LocalDateTime issueEndDate();
}
