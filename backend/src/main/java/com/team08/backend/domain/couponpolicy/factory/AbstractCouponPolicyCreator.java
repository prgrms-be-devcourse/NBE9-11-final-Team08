package com.team08.backend.domain.couponpolicy.factory;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.domain.couponpolicy.entity.DiscountType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

public abstract class AbstractCouponPolicyCreator implements CouponPolicyCreator {

    @Override
    public CouponPolicy create(CouponPolicyCreateRequest request) {
        validateCommon(request);
        validateSpecific(request);
        return createEntity(request);
    }

    // 공통 검증
    private void validateCommon(CouponPolicyCreateRequest request) {
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

    // 타입별 특화 검증
    protected abstract void validateSpecific(CouponPolicyCreateRequest request);

    // 엔티티 생성 위임
    protected abstract CouponPolicy createEntity(CouponPolicyCreateRequest request);
}
