package com.team08.backend.domain.payment.outbox;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.payment.service.PaidCourseStudyMemberService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessOutboxTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long EVENT_ID = 200L;
    private static final Long COURSE_ID = 1000L;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-28T10:00:00Z"), ZONE_ID);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final LocalDateTime PAID_AT = FIXED_NOW.minusMinutes(1);

    @Mock
    private PaymentSuccessOutboxRepository paymentSuccessOutboxRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private PaidCourseStudyMemberService paidCourseStudyMemberService;

    private PaymentSuccessOutboxTransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new PaymentSuccessOutboxTransactionService(
                paymentSuccessOutboxRepository,
                paymentRepository,
                orderRepository,
                orderItemRepository,
                enrollmentRepository,
                paidCourseStudyMemberService,
                FIXED_CLOCK
        );
    }

    @Test
    void processingEventIssuesEnrollmentThenJoinsPaidStudy() {
        PaymentSuccessOutboxEvent event = pendingEvent();
        Order order = paidOrder();
        Payment payment = successfulPayment(order);
        OrderItem orderItem = orderItem(order);
        stubTarget(event, order, payment, orderItem);
        given(enrollmentRepository.findAllByOrder_IdAndStatus(ORDER_ID, EnrollmentStatus.ACTIVE))
                .willReturn(List.of());
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(USER_ID, List.of(COURSE_ID)))
                .willReturn(List.of());
        given(enrollmentRepository.saveAllAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        transactionService.processPending(EVENT_ID);

        ArgumentCaptor<List<Enrollment>> captor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(enrollment -> {
            assertThat(enrollment.getUserId()).isEqualTo(USER_ID);
            assertThat(enrollment.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(enrollment.getOrder()).isSameAs(order);
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            assertThat(enrollment.getEnrolledAt()).isEqualTo(PAID_AT);
        });
        verify(paidCourseStudyMemberService).joinAsMember(USER_ID, List.of(COURSE_ID), PAID_AT);
        assertThat(event.getStatus()).isEqualTo(PaymentSuccessOutboxStatus.SUCCESS);
        assertThat(event.getProcessedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void reprocessingExistingOrderEnrollmentDoesNotCreateDuplicate() {
        PaymentSuccessOutboxEvent event = pendingEvent();
        Order order = paidOrder();
        Payment payment = successfulPayment(order);
        OrderItem orderItem = orderItem(order);
        Enrollment existingEnrollment = Enrollment.createActive(USER_ID, COURSE_ID, order, PAID_AT);
        stubTarget(event, order, payment, orderItem);
        given(enrollmentRepository.findAllByOrder_IdAndStatus(ORDER_ID, EnrollmentStatus.ACTIVE))
                .willReturn(List.of(existingEnrollment));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(USER_ID, List.of(COURSE_ID)))
                .willReturn(List.of(COURSE_ID));

        transactionService.processPending(EVENT_ID);

        verify(enrollmentRepository, never()).saveAllAndFlush(any());
        verify(paidCourseStudyMemberService).joinAsMember(USER_ID, List.of(COURSE_ID), PAID_AT);
        assertThat(event.getStatus()).isEqualTo(PaymentSuccessOutboxStatus.SUCCESS);
    }

    private void stubTarget(
            PaymentSuccessOutboxEvent event,
            Order order,
            Payment payment,
            OrderItem orderItem
    ) {
        given(paymentSuccessOutboxRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(event));
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
    }

    private PaymentSuccessOutboxEvent pendingEvent() {
        PaymentSuccessOutboxEvent event = PaymentSuccessOutboxEvent.paymentSucceeded(PAYMENT_ID, ORDER_ID, USER_ID);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }

    private Order paidOrder() {
        Order order = Order.createPendingPayment(USER_ID, "ORD-20260628190000-ABC12345", PAID_AT.minusHours(1));
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        ReflectionTestUtils.setField(order, "totalPrice", 30_000);
        ReflectionTestUtils.setField(order, "finalPrice", 30_000);
        order.markPaid(PAID_AT);
        return order;
    }

    private Payment successfulPayment(Order order) {
        Payment payment = Payment.createReady(order, PaymentProviderType.TOSS, PAID_AT.minusMinutes(1));
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        payment.markProcessing(PaymentProviderType.TOSS, PAID_AT.minusMinutes(1));
        payment.succeed("payment-key", "CARD", PAID_AT);
        return payment;
    }

    private OrderItem orderItem(Order order) {
        OrderItem orderItem = OrderItem.createSnapshot(order, COURSE_ID, "Spring", 30_000, 0, 30_000, PAID_AT);
        ReflectionTestUtils.setField(orderItem, "id", 1L);
        return orderItem;
    }
}
