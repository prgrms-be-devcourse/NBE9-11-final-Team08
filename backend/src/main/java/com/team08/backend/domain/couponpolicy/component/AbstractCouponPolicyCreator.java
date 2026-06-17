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

    // 타입별 특화 검증
    protected abstract void validateSpecific(CouponPolicyCreateRequest request);

    // 엔티티 생성 위임
    protected abstract CouponPolicy createEntity(CouponPolicyCreateRequest request);
}
