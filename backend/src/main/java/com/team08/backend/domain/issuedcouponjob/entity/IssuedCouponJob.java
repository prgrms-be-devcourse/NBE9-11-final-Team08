package com.team08.backend.domain.issuedcouponjob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "issued_coupon_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCouponJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuedCouponJobStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime lastTriedAt;

    private LocalDateTime completedAt;

    private IssuedCouponJob(Long userId, Long policyId, LocalDateTime requestedAt) {
        this.userId = userId;
        this.policyId = policyId;
        this.status = IssuedCouponJobStatus.REQUESTED;
        this.retryCount = 0;
        this.requestedAt = requestedAt;
    }

    // 쿠폰 발급 작업 생성
    public static IssuedCouponJob request(Long userId, Long policyId, LocalDateTime requestedAt) {
        return new IssuedCouponJob(userId, policyId, requestedAt);
    }

    // 쿠폰 발급 성공 처리
    public void markIssued(LocalDateTime completedAt) {
        this.status = IssuedCouponJobStatus.ISSUED;
        this.failureReason = null;
        this.lastTriedAt = completedAt;
        this.completedAt = completedAt;
    }

    // 쿠폰 발급 재시도 처리
    public void markRetrying(String failureReason, LocalDateTime failedAt, int maxRetryCount) {
        this.retryCount++;
        this.failureReason = failureReason;
        this.lastTriedAt = failedAt;

        if (this.retryCount >= maxRetryCount) {
            this.status = IssuedCouponJobStatus.DEAD;
            this.completedAt = failedAt;
            return;
        }

        this.status = IssuedCouponJobStatus.RETRYING;
        this.completedAt = null;
    }

    public boolean isProcessable() {
        return this.status == IssuedCouponJobStatus.REQUESTED
                || this.status == IssuedCouponJobStatus.RETRYING;
    }
}
