package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.dto.CouponPolicyUpdateRequest;
import com.team08.backend.domain.couponpolicy.entity.AutoIssueType;
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
                request.minOrderAmount(),
                request.validDays(),
                request.couponTarget(),
                request.categoryIds(),
                request.courseIds(),
                request.issueStartDate(),
                request.issueEndDate(),
                request.autoIssueType()
        );
    }

    public void validateForUpdate(CouponPolicyUpdateRequest request, CouponType existingType, CouponTarget existingTarget) {
        validateCommon(
                existingType,
                request.totalQuantity(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                existingTarget,
                request.categoryIds(),
                request.courseIds(),
                request.issueStartDate(),
                request.issueEndDate(),
                request.autoIssueType()
        );
    }

    private void validateCommon(
            CouponType type,
            Integer totalQuantity,
            DiscountType discountType,
            Integer discountValue,
            Integer maxDiscountAmount,
            Integer minOrderAmount,
            Integer validDays,
            CouponTarget target,
            List<Long> categoryIds,
            List<Long> courseIds,
            LocalDateTime issueStartDate,
            LocalDateTime issueEndDate,
            AutoIssueType autoIssueType
    ) {

        // 숫자 범위 검증
        if (totalQuantity != null && totalQuantity < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (discountValue == null || discountValue < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (maxDiscountAmount != null && maxDiscountAmount < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (minOrderAmount != null && minOrderAmount < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (validDays != null && validDays < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 쿠폰 타입별 검증
        if (type == CouponType.FCFS) {
            if (totalQuantity == null || totalQuantity < 1) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (type == CouponType.NORMAL || type == CouponType.ADMIN) {
            if (totalQuantity != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        if (type == CouponType.AUTO) {
            if (autoIssueType == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else {
            if (autoIssueType != null) {
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
