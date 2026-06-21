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
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    private IssuedCouponJob(Long userId, Long policyId, LocalDateTime requestedAt) {
        this.userId = userId;
        this.policyId = policyId;
        this.status = IssuedCouponJobStatus.REQUESTED;
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
        this.completedAt = completedAt;
    }

    // 쿠폰 발급 실패 처리
    public void markFailed(String failureReason, LocalDateTime completedAt) {
        this.status = IssuedCouponJobStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
    }

    public boolean isRequested() {
        return this.status == IssuedCouponJobStatus.REQUESTED;
    }
}
