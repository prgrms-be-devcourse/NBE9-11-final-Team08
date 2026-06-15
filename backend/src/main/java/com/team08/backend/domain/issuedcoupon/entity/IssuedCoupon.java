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

    private IssuedCoupon(Long policyId, Long userId, LocalDateTime expiredAt, LocalDateTime now) {
        this.policyId = policyId;
        this.userId = userId;
        this.status = CouponStatus.ISSUED;
        this.issuedAt = now;
        this.expiredAt = expiredAt;
    }

    // 쿠폰 발급 기록 생성
    public static IssuedCoupon create(CouponPolicy policy, Long userId, LocalDateTime now) {
        return new IssuedCoupon(
                policy.getId(),
                userId,
                policy.calculateExpirationDate(), // TODO: 나중에 Policy 쪽도 Clock 적용 시 파라미터로 now 전달 필요
                now
        );
    }

    // 쿠폰 사용 처리 (단회성)
    public void use(LocalDateTime now) {
        this.status = CouponStatus.USED;
        this.usedAt = now;
    }

    // 쿠폰 사용 기록 (다회성)
    public void recordUsage(LocalDateTime now) {
        this.usedAt = now;
    }

    // 쿠폰 사용 처리 (쿠폰 사용 타입에 따라 결정)
    public void applyUsage(CouponUsageType usageType, LocalDateTime now) {
        if (usageType == CouponUsageType.SINGLE_USE) {
            this.use(now);
        } else {
            this.recordUsage(now);
        }
    }

    // 쿠폰 만료 처리
    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    // 쿠폰 사용 가능 여부 검증
    public void validateUsable(Long userId, LocalDateTime now) {
        if (!this.userId.equals(userId)) {
            throw new CouponNotOwnedException();
        }

        // 상태 체크, 만료 시각 체크
        if (this.status != CouponStatus.ISSUED || this.expiredAt.isBefore(now)) {
            throw new CouponAlreadyUsedOrExpiredException();
        }
    }
}
