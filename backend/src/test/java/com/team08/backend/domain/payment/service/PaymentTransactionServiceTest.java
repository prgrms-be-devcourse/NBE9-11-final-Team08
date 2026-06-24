package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.ordercouponusage.repository.OrderCouponUsageRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentAttempt;
import com.team08.backend.domain.payment.entity.PaymentAttemptStatus;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long ATTEMPT_ID = 200L;
    private static final Long COURSE_ID = 1000L;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZONE_ID);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private IssuedCouponService issuedCouponService;

    @Mock
    private OrderCouponUsageRepository orderCouponUsageRepository;

    private PaymentTransactionService paymentTransactionService;

    @BeforeEach
    void setUp() {
        paymentTransactionService = new PaymentTransactionService(
                paymentRepository,
                paymentAttemptRepository,
                orderRepository,
                orderItemRepository,
                enrollmentRepository,
                issuedCouponService,
                orderCouponUsageRepository,
                FIXED_CLOCK
        );
    }

    @Test
    void paidOrderCannotStartTossPayment() {
        Order order = order(OrderStatus.PAID);
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentTransactionService.prepareTossPayment(USER_ID, ORDER_ID, confirmRequest()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verifyNoInteractions(paymentRepository, paymentAttemptRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void processingPaymentCannotStartTossPaymentAgain() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment payment = Payment.createReady(order, PaymentProviderType.TOSS, FIXED_NOW.minusMinutes(1));
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        payment.markProcessing(PaymentProviderType.TOSS, FIXED_NOW.minusMinutes(1));

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentTransactionService.prepareTossPayment(USER_ID, ORDER_ID, confirmRequest()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));

        verify(paymentAttemptRepository, never()).save(any(PaymentAttempt.class));
    }

    @Test
    void tossPaymentCanBePreparedAndCompletedInSeparatedTransactions() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        AtomicReference<Payment> savedPayment = new AtomicReference<>();
        AtomicReference<PaymentAttempt> savedAttempt = new AtomicReference<>();

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave(savedPayment);
        stubPaymentAttemptSave(savedAttempt);
        stubEnrollmentSaveAll();

        PaymentTransactionService.TossPaymentProcessingContext context =
                paymentTransactionService.prepareTossPayment(USER_ID, ORDER_ID, confirmRequest());

        assertThat(context.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(context.amount()).isEqualTo(30_000);
        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(savedAttempt.get().getStatus()).isEqualTo(PaymentAttemptStatus.REQUESTED);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(savedPayment.get()));
        given(paymentAttemptRepository.findById(ATTEMPT_ID)).willReturn(Optional.of(savedAttempt.get()));

        ConfirmPaymentResponse response = paymentTransactionService.completeTossPayment(
                context,
                tossResponse(order.getOrderNumber(), "DONE", 30_000)
        );

        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedAttempt.get().getStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.enrolledCourseIds()).containsExactly(COURSE_ID);
        verify(enrollmentRepository).saveAll(any());
    }

    @Test
    void tossResultMismatchMarksUnknownWithoutEnrollment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        AtomicReference<Payment> savedPayment = new AtomicReference<>();
        AtomicReference<PaymentAttempt> savedAttempt = new AtomicReference<>();

        PaymentTransactionService.TossPaymentProcessingContext context = prepareTossPayment(
                order,
                orderItem,
                savedPayment,
                savedAttempt
        );
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(savedPayment.get()));
        given(paymentAttemptRepository.findById(ATTEMPT_ID)).willReturn(Optional.of(savedAttempt.get()));

        ConfirmPaymentResponse response = paymentTransactionService.completeTossPayment(
                context,
                tossResponse("ORD-MISMATCH", "DONE", 30_000)
        );

        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(savedAttempt.get().getStatus()).isEqualTo(PaymentAttemptStatus.UNKNOWN);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(enrollmentRepository, never()).saveAll(any());
    }

    @Test
    void tossTimeoutMarksUnknownWithoutEnrollment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        AtomicReference<Payment> savedPayment = new AtomicReference<>();
        AtomicReference<PaymentAttempt> savedAttempt = new AtomicReference<>();

        PaymentTransactionService.TossPaymentProcessingContext context = prepareTossPayment(
                order,
                orderItem,
                savedPayment,
                savedAttempt
        );
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(savedPayment.get()));
        given(paymentAttemptRepository.findById(ATTEMPT_ID)).willReturn(Optional.of(savedAttempt.get()));

        ConfirmPaymentResponse response = paymentTransactionService.failTossPayment(
                context,
                confirmRequest(),
                TossPaymentException.timeout("TOSS_TIMEOUT", "Toss timeout")
        );

        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(savedAttempt.get().getStatus()).isEqualTo(PaymentAttemptStatus.TIMEOUT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(enrollmentRepository, never()).saveAll(any());
    }

    private PaymentTransactionService.TossPaymentProcessingContext prepareTossPayment(
            Order order,
            OrderItem orderItem,
            AtomicReference<Payment> savedPayment,
            AtomicReference<PaymentAttempt> savedAttempt
    ) {
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                USER_ID,
                EnrollmentStatus.ACTIVE,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave(savedPayment);
        stubPaymentAttemptSave(savedAttempt);

        return paymentTransactionService.prepareTossPayment(USER_ID, ORDER_ID, confirmRequest());
    }

    private void stubPaymentSave(AtomicReference<Payment> savedPayment) {
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
            savedPayment.set(payment);
            return payment;
        });
    }

    private void stubPaymentAttemptSave(AtomicReference<PaymentAttempt> savedAttempt) {
        given(paymentAttemptRepository.save(any(PaymentAttempt.class))).willAnswer(invocation -> {
            PaymentAttempt attempt = invocation.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", ATTEMPT_ID);
            savedAttempt.set(attempt);
            return attempt;
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

    private ConfirmPaymentRequest confirmRequest() {
        return new ConfirmPaymentRequest("payment-key", "CARD", 30_000, null);
    }

    private TossPaymentResponse tossResponse(String orderNumber, String status, int amount) {
        return new TossPaymentResponse(
                "payment-key",
                orderNumber,
                status,
                "CARD",
                amount,
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }
}
