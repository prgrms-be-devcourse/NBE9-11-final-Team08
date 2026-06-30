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
import com.team08.backend.domain.payment.entity.PaymentAttempt;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.outbox.PaymentSuccessOutboxService;
import com.team08.backend.domain.payment.provider.PaymentProviderConfirmRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import com.team08.backend.domain.ordercouponusage.repository.OrderCouponUsageRepository;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentProcessingContext;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPreparePaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPreparePaymentResponse;
import com.team08.backend.domain.payment.util.NicepaySignature;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String NICEPAY_CARD_PAY_METHOD = "CARD";
    private static final String NICEPAY_CHARSET = "utf-8";
    private static final DateTimeFormatter NICEPAY_EDI_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentProviderRouter paymentProviderRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final IssuedCouponService issuedCouponService;
    private final OrderCouponUsageRepository orderCouponUsageRepository;
    private final PaidCourseStudyMemberService paidCourseStudyMemberService;
    private final PaymentSuccessOutboxService paymentSuccessOutboxService;
    private final NicepayPaymentProperties nicepayPaymentProperties;
    private final Clock clock;

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findMyPaymentOrderForUpdate(userId, orderId);

        // 주문 상태와 Payment 존재 여부를 함께 확인해 중복 결제를 방어한다.
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        Optional<ConfirmPaymentResponse> idempotentResponse = findIdempotentConfirmResponse(
                order,
                existingPayment,
                request.idempotencyKey()
        );
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }
        validateConfirmableOrder(order);
        validateConfirmablePayment(existingPayment);

        int expectedDiscount = 0;
        if (request.issuedCouponId() != null) {
            expectedDiscount = issuedCouponService.calculateExpectedDiscount(userId, request.issuedCouponId(), order.getTotalPrice()).discountAmount();
        }

        validatePaymentAmount(order, request.amount(), expectedDiscount);

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        if (request.issuedCouponId() != null) {
            int actualDiscount = issuedCouponService.useCouponForOrder(userId, request.issuedCouponId(), order.getTotalPrice());
            order.applyDiscount(actualDiscount);
            orderCouponUsageRepository.save(new OrderCouponUsage(order.getId(), request.issuedCouponId(), actualDiscount));
        }

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Payment savedPayment = processSuccessfulPayment(order, existingPayment, request, paidAt);
        saveSuccessfulMockAttempt(savedPayment, request, paidAt);

        markOrderPaid(order, paidAt);

        paymentSuccessOutboxService.createIfAbsent(savedPayment.getId(), order.getId(), userId);

        return ConfirmPaymentResponse.from(savedPayment, order);
    }

    public ConfirmPaymentResponse confirmTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        return confirmProviderPayment(userId, orderId, PaymentProviderType.TOSS, request);
    }

    public ConfirmPaymentResponse confirmProviderPayment(
            Long userId,
            Long orderId,
            PaymentProviderType providerType,
            ConfirmPaymentRequest request
    ) {
        TossPaymentProcessingContext context = paymentTransactionService.prepareProviderPayment(userId, orderId, providerType, request);
        if (context.isIdempotentReplay()) {
            return context.idempotentResponse();
        }

        try {
            PaymentProviderConfirmRequest providerRequest = new PaymentProviderConfirmRequest(
                    request.paymentKey(),
                    context.orderNumber(),
                    context.amount(),
                    request.authResultCode(),
                    request.authResultMsg(),
                    request.authToken(),
                    request.txTid(),
                    request.mid(),
                    request.moid(),
                    request.signature(),
                    request.nextAppUrl(),
                    request.netCancelUrl(),
                    request.payMethod()
            );
            return paymentTransactionService.completeProviderPayment(
                    context,
                    paymentProviderRouter.confirm(providerType, providerRequest)
            );
        } catch (PaymentProviderException e) {
            return paymentTransactionService.failProviderPayment(context, request, e);
        }
    }

    @Transactional
    public NicepayPreparePaymentResponse prepareNicepayPayment(
            LoginUserDto user,
            Long orderId,
            NicepayPreparePaymentRequest request
    ) {
        validateNicepayCardPayMethod(request.payMethod());
        if (!StringUtils.hasText(nicepayPaymentProperties.mid())
                || !StringUtils.hasText(nicepayPaymentProperties.merchantKey())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Order order = findMyPaymentOrder(user.id(), orderId);
        validateConfirmableOrder(order);
        validateConfirmablePayment(paymentRepository.findByOrder_Id(orderId));

        int expectedDiscount = calculateExpectedDiscount(user.id(), request.issuedCouponId(), order);
        int amount = order.getFinalPrice() - expectedDiscount;
        String ediDate = LocalDateTime.now(clock).format(NICEPAY_EDI_DATE_FORMATTER);
        String signData = NicepaySignature.sha256(
                ediDate + nicepayPaymentProperties.mid() + amount + nicepayPaymentProperties.merchantKey()
        );
        String goodsName = getOrderName(findOrderItems(order));

        return new NicepayPreparePaymentResponse(
                goodsName,
                amount,
                nicepayPaymentProperties.mid(),
                ediDate,
                order.getOrderNumber(),
                signData,
                NICEPAY_CARD_PAY_METHOD,
                user.nickname(),
                "",
                user.email(),
                NICEPAY_CHARSET,
                request.issuedCouponId() == null ? null : String.valueOf(request.issuedCouponId())
        );
    }

    @Transactional
    public PaymentResponse failPayment(Long userId, Long orderId, FailPaymentRequest request) {
        Order order = findMyPaymentOrder(userId, orderId);
        validateFailableOrder(order);

        LocalDateTime declinedAt = LocalDateTime.now(clock);
        Payment savedPayment = processDeclinedPayment(order, request, declinedAt);
        return PaymentResponse.from(savedPayment, order);
    }

    @Transactional
    public PaymentResponse refundPayment(Long userId, Long orderId) {
        Order order = findMyPaymentOrder(userId, orderId);
        validateRefundableOrder(order);

        LocalDateTime refundedAt = LocalDateTime.now(clock);
        Payment payment = refundSuccessfulPayment(order, refundedAt);
        refundOrder(order, refundedAt);
        cancelActiveEnrollmentsForRefund(order, refundedAt);

        return PaymentResponse.from(payment, order);
    }

    private Order findMyPaymentOrder(Long userId, Long orderId) {
        return findMyPaymentOrderForUpdate(userId, orderId);
    }

    private Order findMyPaymentOrderForUpdate(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private List<OrderItem> findOrderItems(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId());
    }

    private void validateConfirmableOrder(Order order) {
        if (order.getStatus() == OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_ORDER_STATUS);
        }
    }

    private void validateConfirmablePayment(Optional<Payment> payment) {
        payment.ifPresent(existingPayment -> {
            if (existingPayment.isCompleted()) {
                throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
            }

            if (!existingPayment.canBeConfirmed()) {
                throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
            }
        });
    }

    private Optional<ConfirmPaymentResponse> findIdempotentConfirmResponse(
            Order order,
            Optional<Payment> payment,
            String idempotencyKey
    ) {
        if (!StringUtils.hasText(idempotencyKey) || payment.isEmpty()) {
            return Optional.empty();
        }

        return paymentAttemptRepository.findByPayment_IdAndIdempotencyKey(payment.get().getId(), idempotencyKey)
                .map(attempt -> toIdempotentResponse(order, payment.get()));
    }

    private ConfirmPaymentResponse toIdempotentResponse(Order order, Payment payment) {
        return ConfirmPaymentResponse.from(payment, order);
    }

    private void validateFailableOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_FAILURE_STATUS);
        }
    }

    private void validateRefundableOrder(Order order) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_ORDER_STATUS);
        }
    }

    private void validatePaymentAmount(Order order, int requestAmount, int expectedDiscount) {
        if (order.getFinalPrice() - expectedDiscount != requestAmount) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private int calculateExpectedDiscount(Long userId, Long issuedCouponId, Order order) {
        if (issuedCouponId == null) {
            return 0;
        }
        return issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, order.getTotalPrice()).discountAmount();
    }

    private void validateNicepayCardPayMethod(String payMethod) {
        if (!NICEPAY_CARD_PAY_METHOD.equals(payMethod)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String getOrderName(List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            return "주문";
        }

        String firstTitle = orderItems.get(0).getCourseTitle();
        if (orderItems.size() == 1) {
            return firstTitle;
        }
        return firstTitle + " 외 " + (orderItems.size() - 1) + "건";
    }

    private void validateDuplicateEnrollment(Long userId, List<OrderItem> orderItems) {
        List<Long> courseIds = orderItems.stream()
                .map(OrderItem::getCourseId)
                .distinct()
                .toList();

        List<Long> existingCourseIds = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                courseIds
        );
        if (!existingCourseIds.isEmpty()) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
    }

    private Payment processSuccessfulPayment(
            Order order,
            Optional<Payment> existingPayment,
            ConfirmPaymentRequest request,
            LocalDateTime paidAt
    ) {
        Payment payment = existingPayment.orElseGet(() -> Payment.createReady(order, paidAt));
        payment.markProcessing(paidAt);
        payment.succeed(request.paymentKey(), request.method(), paidAt);
        return paymentRepository.save(payment);
    }

    private void saveSuccessfulMockAttempt(Payment payment, ConfirmPaymentRequest request, LocalDateTime paidAt) {
        if (!StringUtils.hasText(request.idempotencyKey())) {
            return;
        }

        PaymentAttempt attempt = PaymentAttempt.requested(
                payment,
                PaymentProviderType.MOCK,
                request.amount(),
                request.idempotencyKey(),
                paidAt
        );
        attempt.succeed(request.paymentKey(), paidAt);
        paymentAttemptRepository.save(attempt);
    }

    private Payment processDeclinedPayment(Order order, FailPaymentRequest request, LocalDateTime declinedAt) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseGet(() -> Payment.createReady(order, declinedAt));
        payment.markProcessing(declinedAt);
        payment.decline(request.paymentKey(), request.method(), request.failedReason(), declinedAt);
        return paymentRepository.save(payment);
    }


    private void markOrderPaid(Order order, LocalDateTime paidAt) {
        order.markPaid(paidAt);
    }

    private Payment refundSuccessfulPayment(Order order, LocalDateTime refundedAt) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        validateRefundSupportedProvider(payment);
        if (!payment.canRefund()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        payment.refund(refundedAt);
        return payment;
    }

    private void validateRefundSupportedProvider(Payment payment) {
        if (payment.getProvider() != PaymentProviderType.MOCK) {
            throw new CustomException(ErrorCode.PAYMENT_REFUND_UNSUPPORTED);
        }
    }

    private void refundOrder(Order order, LocalDateTime refundedAt) {
        order.refund(refundedAt);
    }

    private void cancelActiveEnrollmentsForRefund(Order order, LocalDateTime canceledAt) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByOrder_IdAndStatus(order.getId(), EnrollmentStatus.ACTIVE);
        enrollments.forEach(enrollment -> enrollment.cancel(canceledAt));
        paidCourseStudyMemberService.leaveMember(
                order.getUserId(),
                enrollments.stream().map(Enrollment::getCourseId).toList(),
                canceledAt
        );
    }
}
