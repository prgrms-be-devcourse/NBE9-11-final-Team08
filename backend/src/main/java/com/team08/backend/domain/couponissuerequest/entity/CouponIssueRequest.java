package com.team08.backend.domain.couponissuerequest.entity;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issue_requests", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_issue_request_key", columnNames = {"issue_type", "request_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false, length = 100)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponIssueRequestType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponIssueRequestStatus status;

    @Column(nullable = false)
    private long requestedCount;

    @Column(nullable = false)
    private long successCount;

    @Column(nullable = false)
    private long failedCount;

    @Column(nullable = false)
    private long skippedCount;

    private Long requestedBy;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "LONGTEXT")
    private String failureReason;

    private CouponIssueRequest(
            Long policyId,
            String requestKey,
            CouponIssueRequestType issueType,
            Long requestedBy,
            LocalDateTime requestedAt
    ) {
        this.policyId = policyId;
        this.requestKey = requestKey;
        this.issueType = issueType;
        this.status = CouponIssueRequestStatus.REQUESTED;
        this.requestedCount = 0;
        this.successCount = 0;
        this.failedCount = 0;
        this.skippedCount = 0;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    public static CouponIssueRequest request(
            Long policyId,
            String requestKey,
            CouponIssueRequestType issueType,
            Long requestedBy,
            LocalDateTime requestedAt
    ) {
        return new CouponIssueRequest(policyId, requestKey, issueType, requestedBy, requestedAt);
    }

    public void markProcessing(LocalDateTime startedAt) {
        if (this.status == CouponIssueRequestStatus.PROCESSING) {
            return;
        }
        this.status = CouponIssueRequestStatus.PROCESSING;
        this.startedAt = startedAt;
    }

    public void addRequestedCount(long count) {
        this.requestedCount += count;
    }

    public void increaseSuccessCount() {
        this.successCount++;
    }

    public void increaseFailedCount() {
        this.failedCount++;
    }

    public void increaseSkippedCount() {
        this.skippedCount++;
    }

    public void addSkippedCount(long count) {
        this.skippedCount += count;
    }

    public void markCompleted(LocalDateTime completedAt) {
        this.status = CouponIssueRequestStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void markFailed(String failureReason, LocalDateTime completedAt) {
        this.status = CouponIssueRequestStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
    }

    public boolean isFinished() {
        return this.status == CouponIssueRequestStatus.COMPLETED
                || this.status == CouponIssueRequestStatus.FAILED
                || this.status == CouponIssueRequestStatus.CANCELED;
    }

    public void completeIfProcessed(LocalDateTime completedAt) {
        if (this.successCount + this.failedCount + this.skippedCount >= this.requestedCount) {
            markCompleted(completedAt);
        }
    }
}
