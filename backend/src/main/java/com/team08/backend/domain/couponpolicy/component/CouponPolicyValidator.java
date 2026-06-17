package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CouponPolicyValidator {

    public void validate(CouponPolicyCreateRequest request) {
        validateCommon(
                request.couponType(),
                request.totalQuantity(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.couponTarget(),
                request.categoryIds(),
                request.courseIds(),
                request.issueStartDate(),
                request.issueEndDate()
        );
    }

    public void validateForUpdate(CouponPolicyUpdateRequest request, CouponType existingType, CouponTarget existingTarget) {
        validateCommon(
                existingType,
                request.totalQuantity(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                existingTarget,
                request.categoryIds(),
                request.courseIds(),
                request.issueStartDate(),
                request.issueEndDate()
        );
    }

    private void validateCommon(
            CouponType type,
            Integer totalQuantity,
            DiscountType discountType,
            Integer discountValue,
            Integer maxDiscountAmount,
            CouponTarget target,
            List<Long> categoryIds,
            List<Long> courseIds,
            LocalDateTime issueStartDate,
            LocalDateTime issueEndDate
    ) {
        // 쿠폰 타입별 검증
        if (type == CouponType.FCFS) {
            if (totalQuantity == null || totalQuantity < 1) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (type == CouponType.NORMAL) {
            if (totalQuantity != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 할인 타입별 검증
        if (discountType == DiscountType.AMOUNT) {
            if (maxDiscountAmount != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (discountType == DiscountType.PERCENT) {
            if (discountValue > 100) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 적용 대상별 검증
        if (target == CouponTarget.ALL) {
            if ((categoryIds != null && !categoryIds.isEmpty()) || (courseIds != null && !courseIds.isEmpty())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (target == CouponTarget.CATEGORY) {
            if (categoryIds == null || categoryIds.isEmpty() || (courseIds != null && !courseIds.isEmpty())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (target == CouponTarget.COURSE) {
            if (courseIds == null || courseIds.isEmpty() || (categoryIds != null && !categoryIds.isEmpty())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 기간 검증
        if (issueStartDate != null && issueEndDate != null && issueStartDate.isAfter(issueEndDate)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
