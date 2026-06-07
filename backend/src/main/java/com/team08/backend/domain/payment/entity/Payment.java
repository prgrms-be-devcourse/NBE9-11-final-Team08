package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    private String paymentKey;

    private String method;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.READY;

    private LocalDateTime paidAt;

    private String failedReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static Payment ready(Order order, Clock clock) {
        Payment payment = new Payment();
        payment.order = order;
        payment.amount = order.getFinalPrice();
        payment.status = PaymentStatus.READY;
        payment.createdAt = LocalDateTime.now(clock);
        return payment;
    }

    public void succeed(String paymentKey, String method, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = now;
        this.failedReason = null;
        this.updatedAt = now;
    }

    public void fail(String failedReason, Clock clock) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = failedReason;
        this.updatedAt = LocalDateTime.now(clock);
    }
}
