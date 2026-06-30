package com.team08.backend.domain.issuedcoupon.entity;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.domain.issuedcoupon.exception.CouponAlreadyUsedOrExpiredException;
import com.team08.backend.domain.issuedcoupon.exception.CouponNotOwnedException;
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
    public static final String DEFAULT_ISSUE_KEY = "DOWNLOAD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;

    private IssuedCoupon(Long policyId, Long userId, String issueKey, LocalDateTime expiredAt, LocalDateTime now) {
        this.policyId = policyId;
        this.userId = userId;
        this.issueKey = issueKey;
        this.status = CouponStatus.ISSUED;
        this.issuedAt = now;
        this.expiredAt = expiredAt;
    }

    public static IssuedCoupon create(CouponPolicy policy, Long userId, LocalDateTime now) {
        return create(policy, userId, DEFAULT_ISSUE_KEY, now);
    }

    public static IssuedCoupon create(CouponPolicy policy, Long userId, String issueKey, LocalDateTime now) {
        return new IssuedCoupon(
                policy.getId(),
                userId,
                issueKey,
                policy.calculateExpirationDate(now),
                now
        );
    }

    public void use(LocalDateTime now) {
        this.status = CouponStatus.USED;
        this.usedAt = now;
    }

    public void recordUsage(LocalDateTime now) {
        this.usedAt = now;
    }

    public void applyUsage(CouponUsageType usageType, LocalDateTime now) {
        if (usageType == CouponUsageType.SINGLE_USE) {
            this.use(now);
        } else {
            this.recordUsage(now);
        }
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    public void validateUsable(Long userId, LocalDateTime now) {
        if (!this.userId.equals(userId)) {
            throw new CouponNotOwnedException();
        }

        if (this.status != CouponStatus.ISSUED || this.expiredAt.isBefore(now)) {
            throw new CouponAlreadyUsedOrExpiredException();
        }
    }

    public void refund(LocalDateTime now) {
        if (this.status == CouponStatus.USED) {
            if (this.expiredAt.isBefore(now)) {
                this.status = CouponStatus.EXPIRED;
            } else {
                this.status = CouponStatus.ISSUED;
            }
            this.usedAt = null;
        }
    }
}
