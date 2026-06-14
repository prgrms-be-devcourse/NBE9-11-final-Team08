package com.team08.backend.domain.issuedcoupon.entity;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import com.team08.backend.domain.couponpolicy.entity.CouponUsageType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
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

    // 쿠폰 발급 기록 생성
    public static IssuedCoupon create(CouponPolicy policy, Long userId) {
        return new IssuedCoupon(
                policy.getId(),
                userId,
                policy.calculateExpirationDate()
        );
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

    // 쿠폰 사용 처리 (쿠폰 사용 타입에 따라 결정)
    public void applyUsage(CouponUsageType usageType) {
        if (usageType == CouponUsageType.SINGLE_USE) {
            this.use();
        } else {
            this.recordUsage();
        }
    }

    // 쿠폰 사용 가능 여부 검증
    public void validateUsable(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomException(ErrorCode.COUPON_NOT_OWNED);
        }
        if (this.status != CouponStatus.ISSUED) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED_OR_EXPIRED);
        }
    }
}
