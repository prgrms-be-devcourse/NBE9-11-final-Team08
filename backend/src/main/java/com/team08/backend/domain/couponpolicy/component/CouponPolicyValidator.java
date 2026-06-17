package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyValidatable;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CouponPolicyValidator {

    public void validate(CouponPolicyValidatable request) {
        // 쿠폰 타입별 검증
        if (request.couponType() == CouponType.FCFS) {
            if (request.totalQuantity() == null || request.totalQuantity() < 1) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (request.couponType() == CouponType.NORMAL) {
            if (request.totalQuantity() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 할인 타입별 검증
        if (request.discountType() == DiscountType.AMOUNT) {
            if (request.maxDiscountAmount() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (request.discountType() == DiscountType.PERCENT) {
            if (request.discountValue() > 100) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 적용 대상별 검증
        if (request.couponTarget() == CouponTarget.CATEGORY && (request.categoryIds() == null || request.categoryIds().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (request.couponTarget() == CouponTarget.COURSE && (request.courseIds() == null || request.courseIds().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 기간 검증
        if (request.issueStartDate() != null && request.issueEndDate() != null && request.issueStartDate().isAfter(request.issueEndDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
