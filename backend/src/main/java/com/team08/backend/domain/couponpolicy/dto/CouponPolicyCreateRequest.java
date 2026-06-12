package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CouponPolicyCreateRequest(
        @NotBlank(message = "쿠폰 이름은 필수입니다.")
        String name,

        @NotNull(message = "할인 타입은 필수입니다.")
        DiscountType discountType,

        @NotNull(message = "할인 값은 필수입니다.")
        @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
        Integer discountValue,

        @NotNull(message = "유효 기간은 필수입니다.")
        @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
        Integer validDays,

        @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
        Integer totalQuantity,

        Long categoryId,

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
) {
}
