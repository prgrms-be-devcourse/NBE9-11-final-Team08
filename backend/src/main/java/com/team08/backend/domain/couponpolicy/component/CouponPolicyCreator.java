package com.team08.backend.domain.couponpolicy.component;

import com.team08.backend.domain.couponpolicy.dto.CouponPolicyCreateRequest;
import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;

public interface CouponPolicyCreator {

    // 해당 생성기가 요청을 처리할 수 있는지 여부 확인
    boolean supports(CouponPolicyCreateRequest request);

    //요청에 따른 쿠폰 정책 엔티티 생성
    CouponPolicy create(CouponPolicyCreateRequest request);
}
