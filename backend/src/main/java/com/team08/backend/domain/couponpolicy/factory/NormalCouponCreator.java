package com.team08.backend.domain.couponpolicy.factory;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.couponpolicy.service.CouponPolicyValidator;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class NormalCouponCreator extends AbstractCouponPolicyCreator {

    public NormalCouponCreator(CouponPolicyValidator validator) {
        super(validator);
    }

    @Override
    public boolean supports(CouponPolicyCreateRequest request) {
        return request.couponType() == CouponType.NORMAL;
    }

    @Override
    protected void validateSpecific(CouponPolicyCreateRequest request) {
        // 일반 쿠폰 특화 검증: 수량 정보가 없어야 함
        if (request.totalQuantity() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    protected CouponPolicy createEntity(CouponPolicyCreateRequest request) {
        return CouponPolicy.createNormalPolicy(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.minOrderAmount(),
                request.validDays(),
                request.categoryIds(),
                request.courseIds(),
                request.couponTarget(),
                request.usageType(),
                request.isStackable(),
                request.issueStartDate(),
                request.issueEndDate()
        );
    }
}
