package com.team08.backend.domain.issuedcoupon.entity;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "issued_coupons", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "policy_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;

    private IssuedCoupon(Long policyId, Long userId, LocalDateTime expiredAt) {
        this.policyId = policyId;
        this.userId = userId;
        this.status = CouponStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        this.expiredAt = expiredAt;
    }

    public static IssuedCoupon issue(Long policyId, Long userId, LocalDateTime expiredAt) {
        return new IssuedCoupon(policyId, userId, expiredAt);
    }

    // 쿠폰 사용 처리 (단회성)
    public void use() {
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    // 쿠폰 사용 기록 (다회성)
    public void recordUsage() {
        this.usedAt = LocalDateTime.now();
    }
}
