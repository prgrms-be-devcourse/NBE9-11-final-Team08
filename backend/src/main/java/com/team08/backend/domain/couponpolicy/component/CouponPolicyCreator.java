package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;

public interface CouponPolicyCreator {

    boolean supports(CouponPolicyCreateRequest request);

    CouponPolicy create(CouponPolicyCreateRequest request);
}
