package com.team08.backend.domain.couponpolicy.factory;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class FcfsCouponCreator implements CouponPolicyCreator {

    @Override
    public boolean supports(CouponPolicyCreateRequest request) {
        return request.couponType() == CouponType.FCFS;
    }

    @Override
    public CouponPolicy create(CouponPolicyCreateRequest request) {
        validate(request);

        return CouponPolicy.createFcfsPolicy(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                request.totalQuantity(),
                request.categoryId(),
                request.courseIds(),
                request.couponTarget(),
                request.usageType(),
                request.isStackable(),
                request.issueStartDate(),
                request.issueEndDate()
        );
    }

    private void validate(CouponPolicyCreateRequest request) {
        // 선착순 특화 검증: 수량 필수
        if (request.totalQuantity() == null || request.totalQuantity() < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 할인 타입별 공통 검증
        if (request.discountType() == DiscountType.AMOUNT) {
            if (request.maxDiscountAmount() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else if (request.discountType() == DiscountType.PERCENT) {
            if (request.discountValue() > 100) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 적용 대상별 공통 검증
        if (request.couponTarget() == CouponTarget.CATEGORY && request.categoryId() == null) {
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
