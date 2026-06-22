package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
    void failedPaymentKeepsLegacyFailedReasonAndFailureMessage() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime failedAt = requestedAt.plusMinutes(1);
        Payment payment = Payment.createReady(order(), requestedAt);

        payment.fail("payment-key", "CARD", "잔액 부족", failedAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailedReason()).isEqualTo("잔액 부족");
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getFailureMessage()).isEqualTo("잔액 부족");
        assertThat(payment.getUpdatedAt()).isEqualTo(failedAt);
    }

    @Test
    void successClearsFailureFields() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        Payment payment = Payment.createReady(order(), requestedAt);
        payment.fail("payment-key", "CARD", "잔액 부족", requestedAt.plusMinutes(1));

        payment.succeed("payment-key-2", "CARD", requestedAt.plusMinutes(2));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getFailedReason()).isNull();
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getFailureMessage()).isNull();
    }

    private Order order() {
        Order order = Order.createPendingPayment(1L, "ORD-20260618120000-ABC12345", LocalDateTime.parse("2026-06-18T12:00:00"));
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        return order;
    }
}
