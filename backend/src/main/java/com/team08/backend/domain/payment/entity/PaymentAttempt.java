package com.team08.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_attempts",
        indexes = {
                @Index(name = "idx_payment_attempt_payment_created_at", columnList = "payment_id, created_at"),
                @Index(name = "idx_payment_attempt_provider_status", columnList = "provider, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentProviderType provider;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentAttemptStatus status;
    @Column(nullable = false)
    private Integer amount;
    private String paymentKey;
    private String failureCode;
    private String failureMessage;
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PaymentAttempt(
            Long id,
            Payment payment,
            PaymentProviderType provider,
            PaymentAttemptStatus status,
            Integer amount,
            String paymentKey,
            String failureCode,
            String failureMessage,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.payment = payment;
        this.provider = provider;
        this.status = status;
        this.amount = amount;
        this.paymentKey = paymentKey;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentAttempt requested(
            Payment payment,
            PaymentProviderType provider,
            Integer amount,
            LocalDateTime requestedAt
    ) {
        return new PaymentAttempt(
                null,
                payment,
                provider,
                PaymentAttemptStatus.REQUESTED,
                amount,
                null,
                null,
                null,
                requestedAt,
                null,
                requestedAt,
                null
        );
    }

    public void succeed(String paymentKey, LocalDateTime completedAt) {
        this.status = PaymentAttemptStatus.SUCCESS;
        this.paymentKey = paymentKey;
        this.failureCode = null;
        this.failureMessage = null;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void fail(String failureCode, String failureMessage, LocalDateTime completedAt) {
        this.status = PaymentAttemptStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }
}
