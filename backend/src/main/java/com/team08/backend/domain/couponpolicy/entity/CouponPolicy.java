package com.team08.backend.domain.couponpolicy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_policies")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private DiscountType discountType;
    @Column(nullable = false)
    private Integer discountValue;
    private Integer maxDiscountAmount;
    @Column(nullable = false)
    private Integer validDays;
    private Integer totalQuantity;
    private Long categoryId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouponType couponType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouponTarget couponTarget;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CouponUsageType usageType;
    @Column(nullable = false)
    private Boolean isStackable = false;
    private LocalDateTime issueStartDate;
    private LocalDateTime issueEndDate;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
