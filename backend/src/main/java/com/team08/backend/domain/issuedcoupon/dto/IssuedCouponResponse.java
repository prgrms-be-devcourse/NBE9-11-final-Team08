package com.team08.backend.domain.issuedcoupon.dto;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import java.time.LocalDateTime;

public record IssuedCouponResponse(
        Long id,
        Long policyId,
        Long userId,
        CouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt
) {
    public static IssuedCouponResponse from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponResponse(
                issuedCoupon.getId(),
                issuedCoupon.getPolicyId(),
                issuedCoupon.getUserId(),
                issuedCoupon.getStatus(),
                issuedCoupon.getIssuedAt(),
                issuedCoupon.getExpiredAt()
        );
    }
}
