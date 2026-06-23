package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void readyPaymentUsesMockProviderByDefault() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");

        Payment payment = Payment.createReady(order(), requestedAt);

        assertThat(payment.getProvider()).isEqualTo(PaymentProviderType.MOCK);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getAmount()).isEqualTo(30_000);
        assertThat(payment.getCreatedAt()).isEqualTo(requestedAt);
        assertThat(payment.getUpdatedAt()).isEqualTo(requestedAt);
    }

    @Test
    void readyPaymentCanBeMarkedProcessing() {
        LocalDateTime processingStartedAt = LocalDateTime.parse("2026-06-18T12:01:00");
        Payment payment = Payment.createReady(order(), LocalDateTime.parse("2026-06-18T12:00:00"));

        payment.markProcessing(processingStartedAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getUpdatedAt()).isEqualTo(processingStartedAt);
    }

    @Test
    void processingPaymentCanSucceed() {
        LocalDateTime paidAt = LocalDateTime.parse("2026-06-18T12:01:00");
        Payment payment = processingPayment();

        payment.succeed("payment-key", "CARD", paidAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getFailedReason()).isNull();
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getFailureMessage()).isNull();
        assertThat(payment.getUpdatedAt()).isEqualTo(paidAt);
    }

    @Test
    void processingPaymentCanBeDeclined() {
        LocalDateTime declinedAt = LocalDateTime.parse("2026-06-18T12:01:00");
        Payment payment = processingPayment();

        payment.decline("payment-key", "CARD", "잔액 부족", declinedAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getFailedReason()).isEqualTo("잔액 부족");
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getFailureMessage()).isEqualTo("잔액 부족");
        assertThat(payment.getUpdatedAt()).isEqualTo(declinedAt);
    }

    @Test
    void processingPaymentCanBeMarkedUnknown() {
        LocalDateTime unknownAt = LocalDateTime.parse("2026-06-18T12:01:00");
        Payment payment = processingPayment();

        payment.markUnknown("TIMEOUT", "승인 결과 확인 필요", unknownAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(payment.getFailureCode()).isEqualTo("TIMEOUT");
        assertThat(payment.getFailureMessage()).isEqualTo("승인 결과 확인 필요");
        assertThat(payment.getFailedReason()).isEqualTo("승인 결과 확인 필요");
        assertThat(payment.getUpdatedAt()).isEqualTo(unknownAt);
    }

    @Test
    void declinedPaymentCanBeConfirmedAgain() {
        Payment payment = processingPayment();
        payment.decline("잔액 부족", LocalDateTime.parse("2026-06-18T12:01:00"));

        assertThat(payment.canBeConfirmed()).isTrue();

        LocalDateTime retryStartedAt = LocalDateTime.parse("2026-06-18T12:02:00");
        payment.markProcessing(retryStartedAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getUpdatedAt()).isEqualTo(retryStartedAt);
    }

    @Test
    void unknownPaymentCannotBeConfirmedAgain() {
        Payment payment = processingPayment();
        payment.markUnknown("TIMEOUT", "승인 결과 확인 필요", LocalDateTime.parse("2026-06-18T12:01:00"));

        assertThat(payment.canBeConfirmed()).isFalse();
        assertThatThrownBy(() -> payment.markProcessing(LocalDateTime.parse("2026-06-18T12:02:00")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
    }

    @Test
    void successPaymentCanBeRefunded() {
        LocalDateTime refundedAt = LocalDateTime.parse("2026-06-18T12:02:00");
        Payment payment = processingPayment();
        payment.succeed("payment-key", "CARD", LocalDateTime.parse("2026-06-18T12:01:00"));

        payment.refund(refundedAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(payment.getUpdatedAt()).isEqualTo(refundedAt);
    }

    private Payment processingPayment() {
        Payment payment = Payment.createReady(order(), LocalDateTime.parse("2026-06-18T12:00:00"));
        payment.markProcessing(LocalDateTime.parse("2026-06-18T12:00:30"));
        return payment;
    }

    private Order order() {
        Order order = Order.createPendingPayment(1L, "ORD-20260618120000-ABC12345", LocalDateTime.parse("2026-06-18T12:00:00"));
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        return order;
    }
}
