package com.team08.backend.domain.couponpolicy.entity;

import com.team08.backend.domain.couponpolicy.entity.CouponPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_policy_courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_policy_course",
                columnNames = {"coupon_policy_id", "course_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    @Column(nullable = false)
    private Long courseId;

    public CouponPolicyCourse(CouponPolicy couponPolicy, Long courseId) {
        this.couponPolicy = couponPolicy;
        this.courseId = courseId;
    }
}
