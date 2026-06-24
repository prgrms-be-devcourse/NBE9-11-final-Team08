package com.team08.backend.domain.payment.entity;

import com.team08.backend.domain.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAttemptTest {

    @Test
    void requestedAttemptCanSucceed() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = requestedAttempt(requestedAt);

        attempt.succeed("payment-key", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(attempt.getPaymentKey()).isEqualTo("payment-key");
        assertThat(attempt.getFailureCode()).isNull();
        assertThat(attempt.getFailureMessage()).isNull();
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    void requestedAttemptCanBeDeclined() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = requestedAttempt(requestedAt);

        attempt.decline("CARD_DECLINED", "카드 승인 거절", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.DECLINED);
        assertThat(attempt.getFailureCode()).isEqualTo("CARD_DECLINED");
        assertThat(attempt.getFailureMessage()).isEqualTo("카드 승인 거절");
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    void requestedAttemptCanBeMarkedProviderError() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = requestedAttempt(requestedAt);

        attempt.markProviderError("PROVIDER_ERROR", "PG 오류", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROVIDER_ERROR);
        assertThat(attempt.getFailureCode()).isEqualTo("PROVIDER_ERROR");
        assertThat(attempt.getFailureMessage()).isEqualTo("PG 오류");
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    void requestedAttemptCanBeMarkedTimeout() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = requestedAttempt(requestedAt);

        attempt.markTimeout("TIMEOUT", "응답 시간 초과", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.TIMEOUT);
        assertThat(attempt.getFailureCode()).isEqualTo("TIMEOUT");
        assertThat(attempt.getFailureMessage()).isEqualTo("응답 시간 초과");
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    @Test
    void requestedAttemptCanBeMarkedUnknown() {
        LocalDateTime requestedAt = LocalDateTime.parse("2026-06-18T12:00:00");
        LocalDateTime completedAt = requestedAt.plusSeconds(3);
        PaymentAttempt attempt = requestedAttempt(requestedAt);

        attempt.markUnknown("UNKNOWN", "승인 결과 확인 필요", completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.UNKNOWN);
        assertThat(attempt.getFailureCode()).isEqualTo("UNKNOWN");
        assertThat(attempt.getFailureMessage()).isEqualTo("승인 결과 확인 필요");
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(attempt.getUpdatedAt()).isEqualTo(completedAt);
    }

    private PaymentAttempt requestedAttempt(LocalDateTime requestedAt) {
        PaymentAttempt attempt = PaymentAttempt.requested(
                payment(),
                PaymentProviderType.TOSS,
                30_000,
                requestedAt
        );

        assertThat(attempt.getProvider()).isEqualTo(PaymentProviderType.TOSS);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.REQUESTED);
        assertThat(attempt.getAmount()).isEqualTo(30_000);
        assertThat(attempt.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(attempt.getCreatedAt()).isEqualTo(requestedAt);
        return attempt;
    }

    private Payment payment() {
        Order order = Order.createPendingPayment(1L, "ORD-20260618120000-ABC12345", LocalDateTime.parse("2026-06-18T12:00:00"));
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        return Payment.createReady(order, LocalDateTime.parse("2026-06-18T12:00:00"));
    }
}
