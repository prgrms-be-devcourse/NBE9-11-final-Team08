package com.team08.backend.domain.orderpayment;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentStateTransitionTest {

    @Test
    void orderStatusTransitions() {
        Order order = new Order(
                1L,
                1L,
                "ORDER-1",
                10_000,
                0,
                10_000,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.parse("2026-06-11T10:00:00"),
                null,
                null,
                null,
                null,
                LocalDateTime.parse("2026-06-11T10:00:00"),
                null
        );

        LocalDateTime paidAt = LocalDateTime.parse("2026-06-11T10:01:00");
        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:02:00");
        LocalDateTime refundedAt = LocalDateTime.parse("2026-06-11T10:03:00");
        LocalDateTime expiredAt = LocalDateTime.parse("2026-06-11T10:04:00");

        order.markPaid(paidAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);

        order.cancel(canceledAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledAt()).isEqualTo(canceledAt);

        order.refund(refundedAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getRefundedAt()).isEqualTo(refundedAt);

        order.expire(expiredAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.getExpiredAt()).isEqualTo(expiredAt);
    }

    @Test
    void paymentStatusTransitions() {
        Payment payment = new Payment(
                1L,
                1L,
                null,
                null,
                10_000,
                PaymentStatus.READY,
                null,
                "previous failure",
                null,
                null,
                LocalDateTime.parse("2026-06-11T10:00:00"),
                null
        );

        LocalDateTime paidAt = LocalDateTime.parse("2026-06-11T10:01:00");
        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:02:00");
        LocalDateTime refundedAt = LocalDateTime.parse("2026-06-11T10:03:00");

        payment.succeed("payment-key", "CARD", paidAt);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getFailedReason()).isNull();

        payment.fail("network error");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailedReason()).isEqualTo("network error");

        payment.cancel(canceledAt);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isEqualTo(canceledAt);

        payment.refund(refundedAt);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(refundedAt);
    }

    @Test
    void enrollmentStatusTransitions() {
        Enrollment enrollment = new Enrollment(
                1L,
                1L,
                1L,
                1L,
                EnrollmentStatus.ACTIVE,
                LocalDateTime.parse("2026-06-11T10:00:00"),
                null,
                null,
                LocalDateTime.parse("2026-06-11T10:00:00"),
                null
        );

        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:01:00");
        LocalDateTime expiredAt = LocalDateTime.parse("2026-06-11T10:02:00");

        enrollment.cancel(canceledAt);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(enrollment.getCanceledAt()).isEqualTo(canceledAt);

        enrollment.expire(expiredAt);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.EXPIRED);
        assertThat(enrollment.getExpiredAt()).isEqualTo(expiredAt);
    }
}
