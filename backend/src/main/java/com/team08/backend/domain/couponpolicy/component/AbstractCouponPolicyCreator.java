package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractCouponPolicyCreator implements CouponPolicyCreator {

    protected final CouponPolicyValidator couponPolicyValidator;

    @Override
    public CouponPolicy create(CouponPolicyCreateRequest request) {
        couponPolicyValidator.validate(request);
        validateSpecific(request);
        return createEntity(request);
    }

    protected abstract void validateSpecific(CouponPolicyCreateRequest request);

    protected abstract CouponPolicy createEntity(CouponPolicyCreateRequest request);
}
