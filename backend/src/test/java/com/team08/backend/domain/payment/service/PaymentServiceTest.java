package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.FailPaymentRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long COURSE_ID = 1000L;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZONE_ID);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                orderRepository,
                orderItemRepository,
                enrollmentRepository,
                FIXED_CLOCK
        );
    }

    @Test
    void pendingPaymentOrderCanBeConfirmed() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave();
        stubEnrollmentSaveAll();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(payment.getAmount()).isEqualTo(30_000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getPaidAt()).isEqualTo(FIXED_NOW);
        assertThat(payment.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(payment.getUpdatedAt()).isEqualTo(FIXED_NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(FIXED_NOW);
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.amount()).isEqualTo(30_000);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paidAt()).isEqualTo(FIXED_NOW);
        assertThat(response.enrolledCourseIds()).containsExactly(COURSE_ID);

        ArgumentCaptor<Iterable<Enrollment>> enrollmentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(enrollmentRepository).saveAll(enrollmentCaptor.capture());
        List<Enrollment> enrollments = toList(enrollmentCaptor.getValue());
        assertThat(enrollments).singleElement()
                .satisfies(enrollment -> {
                    assertThat(enrollment.getUserId()).isEqualTo(USER_ID);
                    assertThat(enrollment.getCourseId()).isEqualTo(COURSE_ID);
                    assertThat(enrollment.getOrder().getId()).isEqualTo(ORDER_ID);
                    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
                    assertThat(enrollment.getEnrolledAt()).isEqualTo(FIXED_NOW);
                    assertThat(enrollment.getCreatedAt()).isEqualTo(FIXED_NOW);
                });
    }

    @Test
    void orderNotFoundCannotBeConfirmed() {
        given(orderRepository.findByIdAndUserId(ORDER_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(OTHER_USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verifyNoInteractions(paymentRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void paidOrderCannotBeConfirmedAgain() {
        Order order = order(OrderStatus.PAID);
        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verifyNoInteractions(paymentRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void nonPendingPaymentOrderCannotBeConfirmed() {
        Order order = order(OrderStatus.CANCELED);
        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_ORDER_STATUS));

        verifyNoInteractions(paymentRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void existingPaymentPreventsDuplicateConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment(order, PaymentStatus.SUCCESS)));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void activeEnrollmentPreventsConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of(COURSE_ID));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ALREADY_ENROLLED));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getPaidAt()).isNull();
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(enrollmentRepository, never()).saveAll(any());
    }

    @Test
    void multipleOrderItemsCreateMultipleEnrollments() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem firstItem = orderItem(1L, COURSE_ID, 30_000);
        OrderItem secondItem = orderItem(2L, COURSE_ID + 1, 20_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(firstItem, secondItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubPaymentSave();
        stubEnrollmentSaveAll();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        assertThat(response.enrolledCourseIds()).containsExactly(COURSE_ID, COURSE_ID + 1);

        ArgumentCaptor<Iterable<Enrollment>> enrollmentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(enrollmentRepository).saveAll(enrollmentCaptor.capture());
        List<Enrollment> enrollments = toList(enrollmentCaptor.getValue());
        assertThat(enrollments).hasSize(2);
        assertThat(enrollments).extracting(Enrollment::getCourseId)
                .containsExactly(COURSE_ID, COURSE_ID + 1);
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void paymentFailureKeepsOrderPendingAndDoesNotIssueEnrollment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        stubPaymentSave();

        PaymentResponse response = paymentService.failPayment(USER_ID, ORDER_ID, failRequest(30_000));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getFailedReason()).isEqualTo("승인 실패");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void failedPaymentCanBeConfirmedAgain() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment failedPayment = payment(order, PaymentStatus.FAILED);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(failedPayment));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave();
        stubEnrollmentSaveAll();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(failedPayment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.enrolledCourseIds()).containsExactly(COURSE_ID);
        verify(enrollmentRepository).saveAll(any());
    }

    @Test
    void confirmChecksActiveEnrollmentsWithSingleCourseIdLookup() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem firstItem = orderItem(1L, COURSE_ID, 30_000);
        OrderItem secondItem = orderItem(2L, COURSE_ID + 1, 20_000);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(firstItem, secondItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubPaymentSave();
        stubEnrollmentSaveAll();

        paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        ArgumentCaptor<List<Long>> courseIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).findCourseIdsByUserIdAndStatusAndCourseIdIn(
                eq(USER_ID),
                eq(EnrollmentStatus.ACTIVE),
                courseIdsCaptor.capture()
        );
        assertThat(courseIdsCaptor.getValue()).containsExactly(COURSE_ID, COURSE_ID + 1);
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void amountMismatchPreventsConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(29_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void paidOrderCanBeRefunded() {
        Order order = order(OrderStatus.PAID);
        Payment payment = payment(order, PaymentStatus.SUCCESS);
        Enrollment enrollment = Enrollment.createActive(USER_ID, COURSE_ID, order, FIXED_NOW.minusDays(1));

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(enrollmentRepository.findAllByOrder_IdAndStatus(ORDER_ID, EnrollmentStatus.ACTIVE))
                .willReturn(List.of(enrollment));

        PaymentResponse response = paymentService.refundPayment(USER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getRefundedAt()).isEqualTo(FIXED_NOW);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(FIXED_NOW);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELED);
        assertThat(enrollment.getCanceledAt()).isEqualTo(FIXED_NOW);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void refundedOrderCannotBeRefundedAgain() {
        Order order = order(OrderStatus.REFUNDED);

        given(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.refundPayment(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_ORDER_STATUS));

        verifyNoInteractions(paymentRepository, orderItemRepository, enrollmentRepository);
    }

    private void stubPaymentSave() {
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
            return payment;
        });
    }

    private void stubEnrollmentSaveAll() {
        given(enrollmentRepository.saveAll(any())).willAnswer(invocation -> {
            Iterable<Enrollment> enrollments = invocation.getArgument(0);
            List<Enrollment> savedEnrollments = new ArrayList<>();
            long id = 1L;
            for (Enrollment enrollment : enrollments) {
                ReflectionTestUtils.setField(enrollment, "id", id++);
                savedEnrollments.add(enrollment);
            }
            return savedEnrollments;
        });
    }

    private Order order(OrderStatus status) {
        LocalDateTime now = LocalDateTime.parse("2026-06-12T10:00:00");
        Order order = Order.createPendingPayment(USER_ID, "ORD-20260612100000-ABC12345", now);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        ReflectionTestUtils.setField(order, "totalPrice", 30_000);
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private OrderItem orderItem(Long orderItemId, Long courseId, int price) {
        LocalDateTime now = LocalDateTime.parse("2026-06-12T10:00:00");
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = OrderItem.createSnapshot(order, courseId, "Spring", price, 0, price, now);
        ReflectionTestUtils.setField(orderItem, "id", orderItemId);
        return orderItem;
    }

    private Payment payment(Order order, PaymentStatus status) {
        Payment payment = Payment.createReady(order, FIXED_NOW.minusDays(1));
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        if (status == PaymentStatus.SUCCESS) {
            payment.succeed("existing-payment-key", "CARD", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.FAILED) {
            payment.fail("existing-payment-key", "CARD", "기존 실패", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.CANCELED) {
            payment.cancel(FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.REFUNDED) {
            payment.succeed("existing-payment-key", "CARD", FIXED_NOW.minusDays(2));
            payment.refund(FIXED_NOW.minusDays(1));
        }
        return payment;
    }

    private ConfirmPaymentRequest confirmRequest(int amount) {
        return new ConfirmPaymentRequest("payment-key", "CARD", amount);
    }

    private FailPaymentRequest failRequest(int amount) {
        return new FailPaymentRequest("payment-key", "CARD", amount, "승인 실패");
    }

    private List<Enrollment> toList(Iterable<Enrollment> enrollments) {
        List<Enrollment> result = new ArrayList<>();
        enrollments.forEach(result::add);
        return result;
    }
}
