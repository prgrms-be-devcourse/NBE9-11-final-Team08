package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.FailPaymentRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossConfirmPaymentRequest;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentAttempt;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import com.team08.backend.domain.ordercouponusage.repository.OrderCouponUsageRepository;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentProcessingContext;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentTransactionService paymentTransactionService;
    private final IssuedCouponService issuedCouponService;
    private final OrderCouponUsageRepository orderCouponUsageRepository;
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

        // 결제 완료 시점과 수강권 발급 시점을 동일하게 맞춘다.
        List<Enrollment> savedEnrollments = issueEnrollmentsAtPaidTime(userId, order, orderItems, paidAt);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
    }

    public ConfirmPaymentResponse confirmTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        TossPaymentProcessingContext context = paymentTransactionService.prepareTossPayment(userId, orderId, request);
        if (context.isIdempotentReplay()) {
            return context.idempotentResponse();
        }

        try {
            TossConfirmPaymentRequest tossRequest = new TossConfirmPaymentRequest(
                    request.paymentKey(),
                    context.orderNumber(),
                    context.amount()
            );
            return paymentTransactionService.completeTossPayment(context, tossPaymentClient.confirm(tossRequest));
        } catch (TossPaymentException e) {
            return paymentTransactionService.failTossPayment(context, request, e);
        }
    }

    @Transactional
    public PaymentResponse failPayment(Long userId, Long orderId, FailPaymentRequest request) {
        Order order = findMyPaymentOrder(userId, orderId);
        validateFailableOrder(order);

        int expectedDiscount = 0;
        if (request.issuedCouponId() != null) {
            expectedDiscount = issuedCouponService.calculateExpectedDiscount(userId, request.issuedCouponId(), order.getTotalPrice()).discountAmount();
        }

        validatePaymentAmount(order, request.amount(), expectedDiscount);

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
        List<Enrollment> enrollments = payment.isCompleted()
                ? enrollmentRepository.findAllByOrder_IdAndStatus(order.getId(), EnrollmentStatus.ACTIVE)
                : List.of();
        return ConfirmPaymentResponse.from(payment, order, enrollments);
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

    private List<Enrollment> issueEnrollmentsAtPaidTime(Long userId, Order order, List<OrderItem> orderItems, LocalDateTime enrolledAt) {
        List<Enrollment> enrollments = orderItems.stream()
                .map(orderItem -> Enrollment.createActive(userId, orderItem.getCourseId(), order, enrolledAt))
                .toList();

        return enrollmentRepository.saveAll(enrollments);
    }

    private Payment refundSuccessfulPayment(Order order, LocalDateTime refundedAt) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!payment.canRefund()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        payment.refund(refundedAt);
        return payment;
    }

    private void refundOrder(Order order, LocalDateTime refundedAt) {
        order.refund(refundedAt);
    }

    private void cancelActiveEnrollmentsForRefund(Order order, LocalDateTime canceledAt) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByOrder_IdAndStatus(order.getId(), EnrollmentStatus.ACTIVE);
        enrollments.forEach(enrollment -> enrollment.cancel(canceledAt));
    }
}
