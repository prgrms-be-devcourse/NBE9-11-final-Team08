package com.team08.backend.domain.issuedcoupon.dto;

import com.team08.backend.domain.issuedcoupon.entity.CouponStatus;
import com.team08.backend.domain.issuedcoupon.entity.IssuedCoupon;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJob;
import com.team08.backend.domain.issuedcouponjob.entity.IssuedCouponJobStatus;

import java.time.LocalDateTime;

public record CouponDownloadResponse(
        Long issuedCouponId,
        Long jobId,
        Long policyId,
        Long userId,
        CouponStatus status,
        IssuedCouponJobStatus jobStatus,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt
) {
    public static CouponDownloadResponse issued(IssuedCoupon issuedCoupon) {
        return new CouponDownloadResponse(
                issuedCoupon.getId(),
                null,
                issuedCoupon.getPolicyId(),
                issuedCoupon.getUserId(),
                issuedCoupon.getStatus(),
                IssuedCouponJobStatus.ISSUED,
                issuedCoupon.getIssuedAt(),
                issuedCoupon.getExpiredAt()
        );
    }

    public static CouponDownloadResponse requested(Long userId, Long policyId, IssuedCouponJob job) {
        return new CouponDownloadResponse(
                null,
                job.getId(),
                policyId,
                userId,
                null,
                job.getStatus(),
                null,
                null
        );
    }
}
