package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_policy_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_policy_category",
                columnNames = {"coupon_policy_id", "category_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    @Column(nullable = false)
    private Long categoryId;

    public CouponPolicyCategory(CouponPolicy couponPolicy, Long categoryId) {
        this.couponPolicy = couponPolicy;
        this.categoryId = categoryId;
    }
}
