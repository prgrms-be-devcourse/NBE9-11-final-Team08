package com.team08.backend.domain.orderpayment;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderPaymentStateTransitionTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.parse("2026-06-11T10:00:00");

    @Test
    void orderStatusTransitions() {
        LocalDateTime paidAt = LocalDateTime.parse("2026-06-11T10:01:00");
        Order paidOrder = order(OrderStatus.PENDING_PAYMENT);

        paidOrder.markPaid(paidAt);

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOrder.getPaidAt()).isEqualTo(paidAt);
        assertThat(paidOrder.getUpdatedAt()).isEqualTo(paidAt);

        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:02:00");
        Order canceledOrder = order(OrderStatus.PENDING_PAYMENT);

        canceledOrder.cancel(canceledAt);

        assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(canceledOrder.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(canceledOrder.getUpdatedAt()).isEqualTo(canceledAt);

        LocalDateTime refundedAt = LocalDateTime.parse("2026-06-11T10:03:00");
        Order refundedOrder = order(OrderStatus.PAID);

        refundedOrder.refund(refundedAt);

        assertThat(refundedOrder.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(refundedOrder.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(refundedOrder.getUpdatedAt()).isEqualTo(refundedAt);

        LocalDateTime expiredAt = LocalDateTime.parse("2026-06-11T10:04:00");
        Order expiredOrder = order(OrderStatus.PENDING_PAYMENT);

        expiredOrder.expire(expiredAt);

        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(expiredOrder.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(expiredOrder.getUpdatedAt()).isEqualTo(expiredAt);
    }

    @Test
    void invalidOrderStatusTransitionsThrowException() {
        LocalDateTime changedAt = LocalDateTime.parse("2026-06-11T10:01:00");

        assertThatThrownBy(() -> order(OrderStatus.PAID).markPaid(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));
        assertThatThrownBy(() -> order(OrderStatus.PAID).cancel(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));
        assertThatThrownBy(() -> order(OrderStatus.PENDING_PAYMENT).refund(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));
        assertThatThrownBy(() -> order(OrderStatus.PAID).expire(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));
    }

    @Test
    void paymentStatusTransitions() {
        LocalDateTime paidAt = LocalDateTime.parse("2026-06-11T10:01:00");
        Payment succeededPayment = payment(PaymentStatus.READY, "previous failure");

        succeededPayment.succeed("payment-key", "CARD", paidAt);

        assertThat(succeededPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(succeededPayment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(succeededPayment.getMethod()).isEqualTo("CARD");
        assertThat(succeededPayment.getPaidAt()).isEqualTo(paidAt);
        assertThat(succeededPayment.getFailedReason()).isNull();
        assertThat(succeededPayment.getUpdatedAt()).isEqualTo(paidAt);

        LocalDateTime retryPaidAt = LocalDateTime.parse("2026-06-11T10:01:30");
        Payment retrySucceededPayment = payment(PaymentStatus.FAILED, "network error");

        retrySucceededPayment.succeed("retry-payment-key", "CARD", retryPaidAt);

        assertThat(retrySucceededPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(retrySucceededPayment.getPaymentKey()).isEqualTo("retry-payment-key");
        assertThat(retrySucceededPayment.getFailedReason()).isNull();
        assertThat(retrySucceededPayment.getUpdatedAt()).isEqualTo(retryPaidAt);

        LocalDateTime failedAt = LocalDateTime.parse("2026-06-11T10:02:00");
        Payment failedPayment = payment(PaymentStatus.READY, null);

        failedPayment.fail("network error", failedAt);

        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getFailedReason()).isEqualTo("network error");
        assertThat(failedPayment.getUpdatedAt()).isEqualTo(failedAt);

        LocalDateTime retryFailedAt = LocalDateTime.parse("2026-06-11T10:02:30");

        failedPayment.fail("timeout", retryFailedAt);

        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getFailedReason()).isEqualTo("timeout");
        assertThat(failedPayment.getUpdatedAt()).isEqualTo(retryFailedAt);

        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:03:00");
        Payment canceledPayment = payment(PaymentStatus.FAILED, "network error");

        canceledPayment.cancel(canceledAt);

        assertThat(canceledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(canceledPayment.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(canceledPayment.getUpdatedAt()).isEqualTo(canceledAt);

        LocalDateTime refundedAt = LocalDateTime.parse("2026-06-11T10:04:00");
        Payment refundedPayment = payment(PaymentStatus.SUCCESS, null);

        refundedPayment.refund(refundedAt);

        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedPayment.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(refundedPayment.getUpdatedAt()).isEqualTo(refundedAt);
    }

    @Test
    void invalidPaymentStatusTransitionsThrowException() {
        LocalDateTime changedAt = LocalDateTime.parse("2026-06-11T10:01:00");

        assertThatThrownBy(() -> payment(PaymentStatus.SUCCESS, null).succeed("payment-key", "CARD", changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        assertThatThrownBy(() -> payment(PaymentStatus.SUCCESS, null).fail("network error", changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        assertThatThrownBy(() -> payment(PaymentStatus.SUCCESS, null).cancel(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        assertThatThrownBy(() -> payment(PaymentStatus.READY, null).refund(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
    }

    @Test
    void enrollmentStatusTransitions() {
        LocalDateTime canceledAt = LocalDateTime.parse("2026-06-11T10:01:00");
        Enrollment canceledEnrollment = enrollment(EnrollmentStatus.ACTIVE);

        canceledEnrollment.cancel(canceledAt);

        assertThat(canceledEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(canceledEnrollment.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(canceledEnrollment.getUpdatedAt()).isEqualTo(canceledAt);

        LocalDateTime expiredAt = LocalDateTime.parse("2026-06-11T10:02:00");
        Enrollment expiredEnrollment = enrollment(EnrollmentStatus.ACTIVE);

        expiredEnrollment.expire(expiredAt);

        assertThat(expiredEnrollment.getStatus()).isEqualTo(EnrollmentStatus.EXPIRED);
        assertThat(expiredEnrollment.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(expiredEnrollment.getUpdatedAt()).isEqualTo(expiredAt);
    }

    @Test
    void invalidEnrollmentStatusTransitionsThrowException() {
        LocalDateTime changedAt = LocalDateTime.parse("2026-06-11T10:01:00");

        assertThatThrownBy(() -> enrollment(EnrollmentStatus.CANCELED).cancel(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION));
        assertThatThrownBy(() -> enrollment(EnrollmentStatus.EXPIRED).expire(changedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION));
    }

    private Order order(OrderStatus status) {
        Order order = Order.createPendingPayment(1L, "ORDER-1", CREATED_AT);
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "totalPrice", 10_000);
        ReflectionTestUtils.setField(order, "finalPrice", 10_000);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private Payment payment(PaymentStatus status, String failedReason) {
        Payment payment = Payment.createReady(order(OrderStatus.PENDING_PAYMENT), CREATED_AT);
        ReflectionTestUtils.setField(payment, "id", 1L);
        ReflectionTestUtils.setField(payment, "status", status);
        ReflectionTestUtils.setField(payment, "failedReason", failedReason);
        return payment;
    }

    private Enrollment enrollment(EnrollmentStatus status) {
        Enrollment enrollment = Enrollment.createActive(1L, 1L, order(OrderStatus.PAID), CREATED_AT);
        ReflectionTestUtils.setField(enrollment, "id", 1L);
        ReflectionTestUtils.setField(enrollment, "status", status);
        return enrollment;
    }
}
