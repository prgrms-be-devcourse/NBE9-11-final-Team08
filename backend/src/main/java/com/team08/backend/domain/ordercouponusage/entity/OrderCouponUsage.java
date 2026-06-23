package com.team08.backend.domain.ordercouponusage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_coupon_usages")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCouponUsage {
    public OrderCouponUsage(Long orderId, Long issuedCouponId, Integer discountAmount) {
        this.orderId = orderId;
        this.issuedCouponId = issuedCouponId;
        this.discountAmount = discountAmount;
    }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long orderId;
    @Column(nullable = false)
    private Long issuedCouponId;
    @Column(nullable = false)
    private Integer discountAmount;
}
