package com.team08.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, unique = true)
    private String orderNumber;
    @Column(nullable = false)
    private Integer totalPrice;
    @Column(nullable = false)
    private Integer discountPrice = 0;
    @Column(nullable = false)
    private Integer finalPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false)
    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
    private LocalDateTime expiredAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markPaid(LocalDateTime paidAt) {
        this.status = OrderStatus.PAID;
        this.paidAt = paidAt;
    }

    public void cancel(LocalDateTime canceledAt) {
        this.status = OrderStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    public void refund(LocalDateTime refundedAt) {
        this.status = OrderStatus.REFUNDED;
        this.refundedAt = refundedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = OrderStatus.EXPIRED;
        this.expiredAt = expiredAt;
    }
}
