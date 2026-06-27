package com.team08.backend.domain.payment.outbox;

import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_success_outbox_events",
        indexes = @Index(name = "idx_payment_success_outbox_status_id", columnList = "status, id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_success_outbox_payment",
                columnNames = "payment_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentSuccessOutboxEvent extends BaseTimeEntity {

    public static final String PAYMENT_SUCCESS_POST_PROCESSING = "payment-success.post-processing";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSuccessOutboxStatus status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String lastError;

    private LocalDateTime processedAt;

    private PaymentSuccessOutboxEvent(Long paymentId, Long orderId, Long userId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.eventType = PAYMENT_SUCCESS_POST_PROCESSING;
        this.status = PaymentSuccessOutboxStatus.PENDING;
    }

    public static PaymentSuccessOutboxEvent paymentSucceeded(Long paymentId, Long orderId, Long userId) {
        return new PaymentSuccessOutboxEvent(paymentId, orderId, userId);
    }

    public void markProcessing() {
        this.status = PaymentSuccessOutboxStatus.PROCESSING;
        this.lastError = null;
    }

    public void markSuccess(LocalDateTime processedAt) {
        this.status = PaymentSuccessOutboxStatus.SUCCESS;
        this.processedAt = processedAt;
        this.lastError = null;
    }

    public void markFailed(String lastError) {
        this.status = PaymentSuccessOutboxStatus.FAILED;
        this.lastError = lastError;
    }
}
