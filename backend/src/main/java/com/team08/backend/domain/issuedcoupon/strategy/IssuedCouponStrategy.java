package com.team08.backend.domain.issuedcoupon.strategy;

import com.team08.backend.domain.couponpolicy.entity.CouponType;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;

public interface IssuedCouponStrategy {
    // 지원하는 쿠폰 타입 반환
    CouponType getSupportedType();

    // 쿠폰 발급 로직 실행
    IssuedCoupon issue(Long userId, Long policyId);

    // 쿠폰 발급 실패 보상
    default void rollbackIssue(Long userId, Long policyId) {
    }
}
