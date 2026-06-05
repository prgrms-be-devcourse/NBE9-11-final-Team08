package com.team08.backend.domain.order;

import com.team08.backend.domain.coupon.IssuedCoupon;
import com.team08.backend.domain.user.User;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private Integer discountPrice = 0;

    @Column(nullable = false)
    private Integer finalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_coupon_id")
    private IssuedCoupon issuedCoupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    private LocalDateTime expiredAt;

    private LocalDateTime refundedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
