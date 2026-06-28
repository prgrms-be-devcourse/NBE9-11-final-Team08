package com.team08.backend.domain.couponissuerequest.dto;

import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequest;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestStatus;
import com.team08.backend.domain.couponissuerequest.entity.CouponIssueRequestType;

import java.time.LocalDateTime;

public record CouponIssueRequestResponse(
        Long id,
        Long policyId,
        String requestKey,
        CouponIssueRequestType issueType,
        CouponIssueRequestStatus status,
        long requestedCount,
        long successCount,
        long failedCount,
        long skippedCount,
        Long targetUserMaxId,
        Long requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String failureReason
) {

    public static CouponIssueRequestResponse from(CouponIssueRequest request) {
        return new CouponIssueRequestResponse(
                request.getId(),
                request.getPolicyId(),
                request.getRequestKey(),
                request.getIssueType(),
                request.getStatus(),
                request.getRequestedCount(),
                request.getSuccessCount(),
                request.getFailedCount(),
                request.getSkippedCount(),
                request.getTargetUserMaxId(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getFailureReason()
        );
    }
}
