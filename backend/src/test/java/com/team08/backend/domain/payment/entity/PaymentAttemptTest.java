package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAttemptTest {

    @Test
    void paymentAttemptCanBeRequestedAndSucceeded() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = PaymentAttempt.requested(
                payment(),
                PaymentProviderType.MOCK_PRIMARY,
                30_000,
                requestedAt
        );

        attempt.succeed("payment-key", completedAt);

        assertThat(attempt.getProvider()).isEqualTo(PaymentProviderType.MOCK_PRIMARY);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(attempt.getAmount()).isEqualTo(30_000);
        assertThat(attempt.getPaymentKey()).isEqualTo("payment-key");
        assertThat(attempt.getFailureCode()).isNull();
        assertThat(attempt.getFailureMessage()).isNull();
        assertThat(attempt.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getCreatedAt()).isEqualTo(requestedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    void paymentAttemptCanFail() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = PaymentAttempt.requested(
                payment(),
                PaymentProviderType.MOCK_SECONDARY,
                30_000,
                requestedAt
        );

        attempt.fail("MOCK_ERROR", "승인 실패", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.getFailureCode()).isEqualTo("MOCK_ERROR");
        assertThat(attempt.getFailureMessage()).isEqualTo("승인 실패");
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    private Payment payment() {
        Order order = Order.createPendingPayment(1L, "ORD-20260618120000-ABC12345", LocalDateTime.parse("2026-06-18T12:00:00"));
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        return Payment.createReady(order, LocalDateTime.parse("2026-06-18T12:00:00"));
    }
}
