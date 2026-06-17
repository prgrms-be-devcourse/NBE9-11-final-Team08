package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponTarget;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CategoryTargetCouponCreator extends AbstractCouponPolicyCreator {

    public CategoryTargetCouponCreator(CouponPolicyValidator validator) {
        super(validator);
    }

    @Override
    public boolean supports(CouponPolicyCreateRequest request) {
        return request.couponTarget() == CouponTarget.CATEGORY;
    }

    @Override
    protected void validateSpecific(CouponPolicyCreateRequest request) {
        // 카테고리 대상 특화 검증
        if (request.categoryIds() == null || request.categoryIds().isEmpty() ||
                (request.courseIds() != null && !request.courseIds().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    protected CouponPolicy createEntity(CouponPolicyCreateRequest request) {
        return CouponPolicy.createPolicy(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                request.totalQuantity(),
                request.categoryIds(),
                request.courseIds(),
                request.couponType(),
                request.couponTarget(),
                request.usageType(),
                request.isStackable(),
                request.issueStartDate(),
                request.issueEndDate()
        );
    }
}
