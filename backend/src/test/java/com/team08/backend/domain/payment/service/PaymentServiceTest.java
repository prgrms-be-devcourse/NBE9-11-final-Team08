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
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.FailPaymentRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentAttempt;
import com.team08.backend.domain.payment.entity.PaymentAttemptStatus;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.outbox.PaymentSuccessOutboxService;
import com.team08.backend.domain.payment.provider.PaymentProviderConfirmRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderConfirmResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentProviderRouter paymentProviderRouter;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private IssuedCouponService issuedCouponService;

    @Mock
    private OrderCouponUsageRepository orderCouponUsageRepository;

    @Mock
    private PaidCourseStudyMemberService paidCourseStudyMemberService;

    @Mock
    private PaymentSuccessOutboxService paymentSuccessOutboxService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentAttemptRepository,
                orderRepository,
                orderItemRepository,
                enrollmentRepository,
                paymentProviderRouter,
                paymentTransactionService,
                issuedCouponService,
                orderCouponUsageRepository,
                paidCourseStudyMemberService,
                paymentSuccessOutboxService,
                FIXED_CLOCK
        );
    }

    @Test
    void pendingPaymentOrderCanBeConfirmed() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave();

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
        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(enrollmentRepository, never()).saveAll(any());
        verify(enrollmentRepository, never()).saveAllAndFlush(any());
        verify(paymentSuccessOutboxService).createIfAbsent(PAYMENT_ID, ORDER_ID, USER_ID);
        verify(paidCourseStudyMemberService, never()).joinAsMember(any(), any(), any());
    }

    @Test
    @DisplayName("쿠폰을 사용하여 결제를 승인하면 할인 금액이 적용되고 사용 내역이 저장된다")
    void confirmPaymentWithCouponAppliesDiscountAndSavesUsage() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        Long issuedCouponId = 55L;

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID)
        )).willReturn(List.of());

        given(issuedCouponService.calculateExpectedDiscount(USER_ID, issuedCouponId, 30_000))
                .willReturn(new com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse("Coupon", 30_000, 5000, 25_000));
        given(issuedCouponService.useCouponForOrder(USER_ID, issuedCouponId, 30_000)).willReturn(5000);

        stubPaymentSave();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, new ConfirmPaymentRequest("payment-key", "CARD", 25_000, issuedCouponId));

        verify(orderCouponUsageRepository).save(any());
        assertThat(order.getDiscountPrice()).isEqualTo(5000);
        assertThat(order.getFinalPrice()).isEqualTo(25_000);
        assertThat(response.amount()).isEqualTo(25_000);
    }

    @Test
    @DisplayName("전체 회원 쿠폰이 materialize된 직후 해당 쿠폰으로 결제할 수 있다")
    void confirmPayment_afterAllUsersCouponMaterialized_usesCouponAndCompletesPayment() {
        // given
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        Long materializedAllUsersCouponId = 55L;

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        given(issuedCouponService.calculateExpectedDiscount(USER_ID, materializedAllUsersCouponId, 30_000))
                .willReturn(new com.team08.backend.domain.issuedcoupon.dto.ExpectedDiscountResponse(
                        "전체 회원 쿠폰",
                        30_000,
                        5_000,
                        25_000
                ));
        given(issuedCouponService.useCouponForOrder(USER_ID, materializedAllUsersCouponId, 30_000))
                .willReturn(5_000);
        stubPaymentSave();

        // when
        ConfirmPaymentResponse response = paymentService.confirmPayment(
                USER_ID,
                ORDER_ID,
                new ConfirmPaymentRequest("payment-key", "CARD", 25_000, materializedAllUsersCouponId)
        );

        // then
        verify(issuedCouponService).calculateExpectedDiscount(USER_ID, materializedAllUsersCouponId, 30_000);
        verify(issuedCouponService).useCouponForOrder(USER_ID, materializedAllUsersCouponId, 30_000);
        verify(orderCouponUsageRepository).save(any());
        assertThat(order.getDiscountPrice()).isEqualTo(5_000);
        assertThat(order.getFinalPrice()).isEqualTo(25_000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.amount()).isEqualTo(25_000);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void prepareNicepayPaymentCreatesCardFormParametersWithoutMerchantKey() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);
        LoginUserDto user = new LoginUserDto(USER_ID, "user@example.com", "tester", "ROLE_USER");

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));

        NicepayPreparePaymentResponse response = paymentService.prepareNicepayPayment(
                user,
                ORDER_ID,
                new NicepayPreparePaymentRequest("CARD", null)
        );

        assertThat(response.goodsName()).isEqualTo("Spring");
        assertThat(response.amt()).isEqualTo(30_000);
        assertThat(response.mid()).isEqualTo("nicepay00m");
        assertThat(response.moid()).isEqualTo("ORD-20260612100000-ABC12345");
        assertThat(response.payMethod()).isEqualTo("CARD");
        assertThat(response.buyerName()).isEqualTo("tester");
        assertThat(response.buyerEmail()).isEqualTo("user@example.com");
        assertThat(response.signData()).isEqualTo(
                NicepaySignature.sha256("20260618190000" + "nicepay00m" + 30_000 + "merchant-key")
        );
        assertThat(response.toString()).doesNotContain("merchant-key");
    }

    @Test
    void prepareNicepayPaymentAllowsOnlyCardPayMethod() {
        LoginUserDto user = new LoginUserDto(USER_ID, "user@example.com", "tester", "ROLE_USER");

        assertThatThrownBy(() -> paymentService.prepareNicepayPayment(
                user,
                ORDER_ID,
                new NicepayPreparePaymentRequest("BANK", null)
        ))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void orderNotFoundCannotBeConfirmed() {
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(OTHER_USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verifyNoInteractions(paymentRepository, orderItemRepository, enrollmentRepository);
    }

    @Test
    void paidOrderCannotBeConfirmedAgain() {
        Order order = order(OrderStatus.PAID);
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verify(paymentRepository).findByOrder_Id(ORDER_ID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void nonPendingPaymentOrderCannotBeConfirmed() {
        Order order = order(OrderStatus.CANCELED);
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_ORDER_STATUS));

        verify(paymentRepository).findByOrder_Id(ORDER_ID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void existingPaymentPreventsDuplicateConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment(order, PaymentStatus.SUCCESS)));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void existingEnrollmentPreventsConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
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
    void multipleOrderItemsCreateSinglePaymentSuccessOutbox() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem firstItem = orderItem(1L, COURSE_ID, 30_000);
        OrderItem secondItem = orderItem(2L, COURSE_ID + 1, 20_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(firstItem, secondItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubPaymentSave();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(enrollmentRepository, never()).saveAll(any());
        verify(enrollmentRepository, never()).saveAllAndFlush(any());
        verify(paymentSuccessOutboxService).createIfAbsent(PAYMENT_ID, ORDER_ID, USER_ID);
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void declinedPaymentKeepsOrderPendingAndDoesNotIssueEnrollment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        stubPaymentSave();

        PaymentResponse response = paymentService.failPayment(USER_ID, ORDER_ID, failRequest(30_000));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getMethod()).isEqualTo("CARD");
        assertThat(payment.getFailedReason()).isEqualTo("승인 실패");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void amountMismatchStillRecordsDeclinedPayment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        stubPaymentSave();

        PaymentResponse response = paymentService.failPayment(USER_ID, ORDER_ID, failRequest(29_000));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(30_000);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verifyNoInteractions(issuedCouponService);
    }

    @Test
    void declinedPaymentCanBeConfirmedAgain() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment declinedPayment = payment(order, PaymentStatus.DECLINED);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(declinedPayment));
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        assertThat(declinedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(declinedPayment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(enrollmentRepository, never()).saveAll(any());
        verify(paymentSuccessOutboxService).createIfAbsent(PAYMENT_ID, ORDER_ID, USER_ID);
    }

    @Test
    void tossConfirmDelegatesToTransactionServiceAndCallsTossWithOrderNumber() {
        ConfirmPaymentRequest request = confirmRequest(30_000);
        PaymentTransactionService.TossPaymentProcessingContext context = PaymentTransactionService.TossPaymentProcessingContext.requested(
                USER_ID,
                ORDER_ID,
                "ORD-20260612100000-ABC12345",
                30_000,
                PAYMENT_ID,
                200L,
                null,
                0
        );
        PaymentProviderConfirmResponse providerResponse = providerResponse(context.orderNumber(), "DONE", 30_000);
        ConfirmPaymentResponse expectedResponse = confirmSuccessResponse();

        given(paymentTransactionService.prepareProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.TOSS, request)).willReturn(context);
        given(paymentProviderRouter.confirm(eq(PaymentProviderType.TOSS), any(PaymentProviderConfirmRequest.class))).willReturn(providerResponse);
        given(paymentTransactionService.completeProviderPayment(context, providerResponse)).willReturn(expectedResponse);

        ConfirmPaymentResponse response = paymentService.confirmTossPayment(USER_ID, ORDER_ID, request);

        ArgumentCaptor<PaymentProviderConfirmRequest> providerRequestCaptor = ArgumentCaptor.forClass(PaymentProviderConfirmRequest.class);
        verify(paymentProviderRouter).confirm(eq(PaymentProviderType.TOSS), providerRequestCaptor.capture());
        assertThat(providerRequestCaptor.getValue().paymentKey()).isEqualTo("payment-key");
        assertThat(providerRequestCaptor.getValue().orderId()).isEqualTo(context.orderNumber());
        assertThat(providerRequestCaptor.getValue().amount()).isEqualTo(30_000);
        verify(paymentTransactionService).completeProviderPayment(context, providerResponse);
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    void tossConfirmFailureDelegatesToFailureTransaction() {
        ConfirmPaymentRequest request = confirmRequest(30_000);
        PaymentTransactionService.TossPaymentProcessingContext context = PaymentTransactionService.TossPaymentProcessingContext.requested(
                USER_ID,
                ORDER_ID,
                "ORD-20260612100000-ABC12345",
                30_000,
                PAYMENT_ID,
                200L,
                null,
                0
        );
        PaymentProviderException exception = PaymentProviderException.timeout("TOSS_TIMEOUT", "Toss timeout");
        ConfirmPaymentResponse expectedResponse = pendingPaymentResponse(PaymentStatus.UNKNOWN);

        given(paymentTransactionService.prepareProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.TOSS, request)).willReturn(context);
        given(paymentProviderRouter.confirm(eq(PaymentProviderType.TOSS), any(PaymentProviderConfirmRequest.class))).willThrow(exception);
        given(paymentTransactionService.failProviderPayment(context, request, exception)).willReturn(expectedResponse);

        ConfirmPaymentResponse response = paymentService.confirmTossPayment(USER_ID, ORDER_ID, request);

        verify(paymentTransactionService).failProviderPayment(context, request, exception);
        verify(paymentTransactionService, never()).completeProviderPayment(any(), any());
        verify(paymentProviderRouter, times(1)).confirm(eq(PaymentProviderType.TOSS), any(PaymentProviderConfirmRequest.class));
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    void providerConfirmCallsOnlySelectedProviderWithoutFallback() {
        ConfirmPaymentRequest request = confirmRequest(30_000);
        PaymentTransactionService.TossPaymentProcessingContext context = PaymentTransactionService.TossPaymentProcessingContext.requested(
                PaymentProviderType.NICEPAY,
                USER_ID,
                ORDER_ID,
                "ORD-20260612100000-ABC12345",
                30_000,
                PAYMENT_ID,
                200L,
                null,
                0
        );
        PaymentProviderConfirmResponse providerResponse = providerResponse(context.orderNumber(), "DONE", 30_000);
        ConfirmPaymentResponse expectedResponse = confirmSuccessResponse();

        given(paymentTransactionService.prepareProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.NICEPAY, request))
                .willReturn(context);
        given(paymentProviderRouter.confirm(eq(PaymentProviderType.NICEPAY), any(PaymentProviderConfirmRequest.class)))
                .willReturn(providerResponse);
        given(paymentTransactionService.completeProviderPayment(context, providerResponse)).willReturn(expectedResponse);

        ConfirmPaymentResponse response = paymentService.confirmProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.NICEPAY, request);

        verify(paymentProviderRouter).confirm(eq(PaymentProviderType.NICEPAY), any(PaymentProviderConfirmRequest.class));
        verify(paymentProviderRouter, never()).confirm(eq(PaymentProviderType.TOSS), any(PaymentProviderConfirmRequest.class));
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    void providerTimeoutDoesNotFallbackToOtherProvider() {
        ConfirmPaymentRequest request = confirmRequest(30_000);
        PaymentTransactionService.TossPaymentProcessingContext context = PaymentTransactionService.TossPaymentProcessingContext.requested(
                PaymentProviderType.NICEPAY,
                USER_ID,
                ORDER_ID,
                "ORD-20260612100000-ABC12345",
                30_000,
                PAYMENT_ID,
                200L,
                null,
                0
        );
        PaymentProviderException exception = PaymentProviderException.timeout("NICEPAY_TIMEOUT", "NICEPAY timeout");
        ConfirmPaymentResponse expectedResponse = pendingPaymentResponse(PaymentStatus.UNKNOWN);

        given(paymentTransactionService.prepareProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.NICEPAY, request))
                .willReturn(context);
        given(paymentProviderRouter.confirm(eq(PaymentProviderType.NICEPAY), any(PaymentProviderConfirmRequest.class)))
                .willThrow(exception);
        given(paymentTransactionService.failProviderPayment(context, request, exception)).willReturn(expectedResponse);

        ConfirmPaymentResponse response = paymentService.confirmProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.NICEPAY, request);

        verify(paymentProviderRouter).confirm(eq(PaymentProviderType.NICEPAY), any(PaymentProviderConfirmRequest.class));
        verify(paymentProviderRouter, never()).confirm(eq(PaymentProviderType.TOSS), any(PaymentProviderConfirmRequest.class));
        verify(paymentTransactionService).failProviderPayment(context, request, exception);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void sameIdempotencyKeyReplayKeepsEnrollmentIdsEmpty() {
        Order order = order(OrderStatus.PAID);
        Payment payment = payment(order, PaymentStatus.SUCCESS);
        PaymentAttempt attempt = paymentAttempt(payment, "idem-1");

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(paymentAttemptRepository.findByPayment_IdAndIdempotencyKey(PAYMENT_ID, "idem-1")).willReturn(Optional.of(attempt));

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000, "idem-1"));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.enrolledCourseIds()).isEmpty();
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentAttemptRepository, never()).save(any(PaymentAttempt.class));
        verify(enrollmentRepository, never()).findAllByOrder_IdAndStatus(any(), any());
    }

    @Test
    void successPaymentWithDifferentIdempotencyKeyCannotBeConfirmedAgain() {
        Order order = order(OrderStatus.PAID);
        Payment payment = payment(order, PaymentStatus.SUCCESS);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(paymentAttemptRepository.findByPayment_IdAndIdempotencyKey(PAYMENT_ID, "idem-2")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000, "idem-2")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_PAID));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentAttemptRepository, never()).save(any(PaymentAttempt.class));
    }

    @Test
    void processingPaymentWithDifferentIdempotencyKeyCannotBeConfirmedAgain() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment payment = Payment.createReady(order, FIXED_NOW.minusMinutes(1));
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        payment.markProcessing(FIXED_NOW.minusMinutes(1));

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(payment));
        given(paymentAttemptRepository.findByPayment_IdAndIdempotencyKey(PAYMENT_ID, "idem-2")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000, "idem-2")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentAttemptRepository, never()).save(any(PaymentAttempt.class));
    }

    @Test
    void declinedPaymentCanBeRetriedWithDifferentIdempotencyKey() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment declinedPayment = payment(order, PaymentStatus.DECLINED);
        OrderItem orderItem = orderItem(1L, COURSE_ID, 30_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(declinedPayment));
        given(paymentAttemptRepository.findByPayment_IdAndIdempotencyKey(PAYMENT_ID, "idem-2")).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(orderItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID)
        )).willReturn(List.of());
        stubPaymentSave();
        stubPaymentAttemptSave();

        ConfirmPaymentResponse response = paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000, "idem-2"));

        assertThat(declinedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
    }

    @Test
    void tossSameIdempotencyKeyReplayDoesNotCallTossClient() {
        ConfirmPaymentResponse expectedResponse = pendingPaymentResponse(PaymentStatus.SUCCESS);
        PaymentTransactionService.TossPaymentProcessingContext context =
                PaymentTransactionService.TossPaymentProcessingContext.replay(expectedResponse);

        given(paymentTransactionService.prepareProviderPayment(USER_ID, ORDER_ID, PaymentProviderType.TOSS, confirmRequest(30_000, "idem-1")))
                .willReturn(context);

        ConfirmPaymentResponse response = paymentService.confirmTossPayment(USER_ID, ORDER_ID, confirmRequest(30_000, "idem-1"));

        assertThat(response).isSameAs(expectedResponse);
        verifyNoInteractions(paymentProviderRouter);
        verify(paymentTransactionService, never()).completeProviderPayment(any(), any());
        verify(paymentTransactionService, never()).failProviderPayment(any(), any(), any());
    }

    @Test
    void unknownPaymentCannotBeConfirmedAndDoesNotIssueEnrollment() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment unknownPayment = payment(order, PaymentStatus.UNKNOWN);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.of(unknownPayment));

        assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(orderItemRepository, enrollmentRepository);
    }

    @Test
    void confirmChecksExistingEnrollmentsWithSingleCourseIdLookup() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        OrderItem firstItem = orderItem(1L, COURSE_ID, 30_000);
        OrderItem secondItem = orderItem(2L, COURSE_ID + 1, 20_000);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrder_Id(ORDER_ID)).willReturn(Optional.empty());
        given(orderItemRepository.findAllByOrderId(ORDER_ID)).willReturn(List.of(firstItem, secondItem));
        given(enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                USER_ID,
                List.of(COURSE_ID, COURSE_ID + 1)
        )).willReturn(List.of());
        stubPaymentSave();

        paymentService.confirmPayment(USER_ID, ORDER_ID, confirmRequest(30_000));

        ArgumentCaptor<List<Long>> courseIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).findCourseIdsByUserIdAndCourseIdIn(
                eq(USER_ID),
                courseIdsCaptor.capture()
        );
        assertThat(courseIdsCaptor.getValue()).containsExactly(COURSE_ID, COURSE_ID + 1);
        verify(enrollmentRepository, never())
                .existsByUserIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void amountMismatchPreventsConfirm() {
        Order order = order(OrderStatus.PENDING_PAYMENT);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
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

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));
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
        verify(paidCourseStudyMemberService).leaveMember(USER_ID, List.of(COURSE_ID), FIXED_NOW);
    }

    @Test
    void refundedOrderCannotBeRefundedAgain() {
        Order order = order(OrderStatus.REFUNDED);

        given(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).willReturn(Optional.of(order));

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

    private void stubPaymentAttemptSave() {
        given(paymentAttemptRepository.save(any(PaymentAttempt.class))).willAnswer(invocation -> invocation.getArgument(0));
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
            payment.markProcessing(FIXED_NOW.minusDays(1));
            payment.succeed("existing-payment-key", "CARD", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.DECLINED) {
            payment.markProcessing(FIXED_NOW.minusDays(1));
            payment.decline("existing-payment-key", "CARD", "기존 거절", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.UNKNOWN) {
            payment.markProcessing(FIXED_NOW.minusDays(1));
            payment.markUnknown("TIMEOUT", "승인 결과 확인 필요", FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.CANCELED) {
            payment.cancel(FIXED_NOW.minusDays(1));
        } else if (status == PaymentStatus.REFUNDED) {
            payment.markProcessing(FIXED_NOW.minusDays(2));
            payment.succeed("existing-payment-key", "CARD", FIXED_NOW.minusDays(2));
            payment.refund(FIXED_NOW.minusDays(1));
        }
        return payment;
    }

    private ConfirmPaymentRequest confirmRequest(int amount) {
        return new ConfirmPaymentRequest("payment-key", "CARD", amount, null);
    }

    private ConfirmPaymentRequest confirmRequest(int amount, String idempotencyKey) {
        return new ConfirmPaymentRequest("payment-key", "CARD", amount, null, idempotencyKey);
    }

    private PaymentAttempt paymentAttempt(Payment payment, String idempotencyKey) {
        PaymentAttempt attempt = PaymentAttempt.requested(
                payment,
                PaymentProviderType.MOCK,
                payment.getAmount(),
                idempotencyKey,
                FIXED_NOW.minusMinutes(1)
        );
        ReflectionTestUtils.setField(attempt, "id", 200L);
        attempt.succeed(payment.getPaymentKey(), FIXED_NOW.minusMinutes(1));
        return attempt;
    }

    private PaymentProviderConfirmResponse providerResponse(String orderNumber, String status, int amount) {
        return new PaymentProviderConfirmResponse(
                "payment-key",
                orderNumber,
                status,
                "CARD",
                amount,
                java.time.OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }

    private ConfirmPaymentResponse confirmSuccessResponse() {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment payment = payment(order, PaymentStatus.SUCCESS);
        order.markPaid(FIXED_NOW);
        return ConfirmPaymentResponse.from(payment, order);
    }

    private ConfirmPaymentResponse pendingPaymentResponse(PaymentStatus paymentStatus) {
        Order order = order(OrderStatus.PENDING_PAYMENT);
        Payment payment = payment(order, paymentStatus);
        return ConfirmPaymentResponse.from(payment, order);
    }

    private FailPaymentRequest failRequest(int amount) {
        return new FailPaymentRequest("payment-key", "CARD", amount, "승인 실패", null);
    }

}
