package com.team08.backend.domain.couponpolicy.factory;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class FcfsCouponCreator extends AbstractCouponPolicyCreator {

    @Override
    public boolean supports(CouponPolicyCreateRequest request) {
        return request.couponType() == CouponType.FCFS;
    }

    @Override
    protected void validateSpecific(CouponPolicyCreateRequest request) {
        // 선착순 특화 검증: 수량 필수
        if (request.totalQuantity() == null || request.totalQuantity() < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    protected CouponPolicy createEntity(CouponPolicyCreateRequest request) {
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
}
