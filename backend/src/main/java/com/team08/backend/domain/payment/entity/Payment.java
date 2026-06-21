package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    private String paymentKey;
    private String method;
    @Column(nullable = false)
    private Integer amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentProviderType provider;
    private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String failureCode;
    private String failureMessage;
    private String failedReason;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Payment(
            Long id,
            Order order,
            String paymentKey,
            String method,
            Integer amount,
            PaymentProviderType provider,
            String idempotencyKey,
            PaymentStatus status,
            LocalDateTime paidAt,
            String failureCode,
            String failureMessage,
            String failedReason,
            LocalDateTime canceledAt,
            LocalDateTime refundedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.order = order;
        this.paymentKey = paymentKey;
        this.method = method;
        this.amount = amount;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.paidAt = paidAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.failedReason = failedReason;
        this.canceledAt = canceledAt;
        this.refundedAt = refundedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createReady(Order order, LocalDateTime requestedAt) {
        return new Payment(
                null,
                order,
                null,
                null,
                order.getFinalPrice(),
                PaymentProviderType.MOCK,
                null,
                PaymentStatus.READY,
                null,
                null,
                null,
                null,
                null,
                null,
                requestedAt,
                null
        );
    }

    public void succeed(String paymentKey, String method, LocalDateTime paidAt) {
        validateStatus(PaymentStatus.READY, PaymentStatus.FAILED);
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
        this.failureCode = null;
        this.failureMessage = null;
        this.failedReason = null;
        this.updatedAt = paidAt;
    }

    public void fail(String failedReason, LocalDateTime failedAt) {
        fail(null, null, failedReason, failedAt);
    }

    public void fail(String paymentKey, String method, String failedReason, LocalDateTime failedAt) {
        validateStatus(PaymentStatus.READY, PaymentStatus.FAILED);
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.FAILED;
        this.failureCode = null;
        this.failureMessage = failedReason;
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

    public boolean isCompleted() {
        return this.status == PaymentStatus.SUCCESS;
    }

    public boolean canBeConfirmed() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.FAILED;
    }

    public boolean canCancelBeforePaid() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.FAILED;
    }

    public boolean canRefund() {
        return this.status == PaymentStatus.SUCCESS;
    }

    private void validateStatus(PaymentStatus... expectedStatuses) {
        for (PaymentStatus expectedStatus : expectedStatuses) {
            if (this.status == expectedStatus) {
                return;
            }
        }
        throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
    }
}
