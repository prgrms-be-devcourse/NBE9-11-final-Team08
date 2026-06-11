package com.team08.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long orderId;
    private String paymentKey;
    private String method;
    @Column(nullable = false)
    private Integer amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String failedReason;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void succeed(String paymentKey, String method, LocalDateTime paidAt) {
        validateStatus(PaymentStatus.READY, PaymentStatus.FAILED);
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
        this.failedReason = null;
        this.updatedAt = paidAt;
    }

    public void fail(String failedReason, LocalDateTime failedAt) {
        validateStatus(PaymentStatus.READY, PaymentStatus.FAILED);
        this.status = PaymentStatus.FAILED;
        this.failedReason = failedReason;
        this.updatedAt = failedAt;
    }

    public void cancel(LocalDateTime canceledAt) {
        validateStatus(PaymentStatus.READY, PaymentStatus.FAILED);
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
        this.updatedAt = canceledAt;
    }

    public void refund(LocalDateTime refundedAt) {
        validateStatus(PaymentStatus.SUCCESS);
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = refundedAt;
        this.updatedAt = refundedAt;
    }

    private void validateStatus(PaymentStatus... expectedStatuses) {
        for (PaymentStatus expectedStatus : expectedStatuses) {
            if (this.status == expectedStatus) {
                return;
            }
        }
        throw new IllegalStateException("Invalid payment status transition.");
    }
}
