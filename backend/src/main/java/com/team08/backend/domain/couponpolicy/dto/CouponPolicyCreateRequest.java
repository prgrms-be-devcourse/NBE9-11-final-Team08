package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CouponPolicyCreateRequest(
        @NotBlank(message = "쿠폰 이름은 필수입니다.")
        String name,

        @NotNull(message = "할인 타입은 필수입니다.")
        DiscountType discountType,

        @NotNull(message = "할인 값은 필수입니다.")
        @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
        Integer discountValue,

        Integer maxDiscountAmount,

        Integer minOrderAmount,

        Integer validDays, // null이면 무기한

        Integer totalQuantity, // null이면 무제한

        List<Long> categoryIds,

        List<Long> courseIds,

        @NotNull(message = "쿠폰 발급 타입은 필수입니다.")
        CouponType couponType,

        @NotNull(message = "쿠폰 적용 대상은 필수입니다.")
        CouponTarget couponTarget,

        @NotNull(message = "쿠폰 사용 타입은 필수입니다.")
        CouponUsageType usageType,

        @NotNull(message = "중복 적용 여부는 필수입니다.")
        Boolean isStackable,

        LocalDateTime issueStartDate,

        LocalDateTime issueEndDate
) implements CouponPolicyValidatable {
}
