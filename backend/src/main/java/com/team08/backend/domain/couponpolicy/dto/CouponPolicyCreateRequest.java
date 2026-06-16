package com.team08.backend.domain.couponpolicy.dto;

import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

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

        Integer validDays,

        @Min(value = 1, message = "총 수량은 1개 이상이어야 합니다.")
        Integer totalQuantity, // null이면 무제한

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
    @AssertTrue(message = "발급 시작일은 종료일보다 이전이어야 합니다.")
    public boolean isValidIssueDate() {
        if (issueStartDate == null || issueEndDate == null) {
            return true;
        }
        return issueStartDate.isBefore(issueEndDate);
    }

    @AssertTrue(message = "퍼센트 할인 값은 100 이하이어야 합니다.")
    public boolean isDiscountValueValid() {
        if (discountType == DiscountType.PERCENT) {
            return discountValue <= 100;
        }
        return true;
    }

    @AssertTrue(message = "카테고리 할인인 경우 카테고리 ID는 필수입니다.")
    public boolean isCategoryValid() {
        if (couponTarget == CouponTarget.CATEGORY) {
            return categoryId != null;
        }
        return true;
    }

    @AssertTrue(message = "선착순 쿠폰은 총 수량이 필수입니다.")
    public boolean isQuantityValid() {
        if (couponType == CouponType.FCFS) {
            return totalQuantity != null && totalQuantity > 0;
        }
        return true;
    }
}
