package com.team08.backend.domain.order.entity;

import com.team08.backend.domain.coupon.entity.IssuedCoupon;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
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

    public static Order create(User user, String orderNumber, Integer totalPrice, Integer discountPrice, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);

        Order order = new Order();
        order.user = user;
        order.orderNumber = orderNumber;
        order.totalPrice = totalPrice;
        order.discountPrice = discountPrice;
        order.finalPrice = totalPrice - discountPrice;
        order.status = OrderStatus.PENDING_PAYMENT;
        order.orderedAt = now;
        order.createdAt = now;
        return order;
    }

    public void markPaid(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        this.status = OrderStatus.PAID;
        this.paidAt = now;
        this.updatedAt = now;
    }

    public void cancel(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        this.status = OrderStatus.CANCELED;
        this.canceledAt = now;
        this.updatedAt = now;
    }
}
