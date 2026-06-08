package com.team08.backend.domain.coupon.entity;

import com.team08.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "issued_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CouponPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;

    @Builder
    public IssuedCoupon(Long id, CouponPolicy policy, User user, CouponStatus status, LocalDateTime issuedAt, LocalDateTime expiredAt, LocalDateTime usedAt) {
        this.id = id;
        this.policy = policy;
        this.user = user;
        // 쿠폰 상태가 안 들어오면 기본값으로 'ISSUED(발급됨)' 상태를 넣음 (CouponStatus에 ISSUED가 있다고 가정)
        this.status = status != null ? status : CouponStatus.ISSUED;
        // 발급 시간은 현재 시간으로 고정
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
    }

    // [시스템] 쿠폰 사용 처리 (ISSUED -> USED)
    public void use() {
        if (this.status != CouponStatus.ISSUED) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    // 쿠폰 만료 처리 (ISSUED -> EXPIRED)
    public void expire() {
        if (this.status == CouponStatus.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰은 만료 처리할 수 없습니다.");
        }
        this.status = CouponStatus.EXPIRED;
    }
}
