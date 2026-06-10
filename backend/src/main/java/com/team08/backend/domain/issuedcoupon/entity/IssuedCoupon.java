package com.team08.backend.domain.issuedcoupon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "issued_coupons", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "policy_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long policyId;
    @Column(nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouponStatus status;
    @Column(nullable = false)
    private LocalDateTime issuedAt;
    @Column(nullable = false)
    private LocalDateTime expiredAt;
    private LocalDateTime usedAt;
}
