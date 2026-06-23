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
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentAttempt;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    private final Clock clock;

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findMyPaymentOrder(userId, orderId);

        // 주문 상태와 Payment 존재 여부를 함께 확인해 중복 결제를 방어한다.
        validateConfirmableOrder(order);
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        validateConfirmablePayment(existingPayment);
        validatePaymentAmount(order, request.amount());

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Payment savedPayment = processSuccessfulPayment(order, existingPayment, request, paidAt);

        markOrderPaid(order, paidAt);

        // 결제 완료 시점과 수강권 발급 시점을 동일하게 맞춘다.
        List<Enrollment> savedEnrollments = issueEnrollmentsAtPaidTime(userId, order, orderItems, paidAt);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
    }

    @Transactional
    public ConfirmPaymentResponse confirmTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findMyPaymentOrder(userId, orderId);

        validateConfirmableOrder(order);
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        validateConfirmablePayment(existingPayment);
        validatePaymentAmount(order, request.amount());

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime requestedAt = LocalDateTime.now(clock);
        Payment payment = processTossPaymentRequested(order, existingPayment, requestedAt);
        PaymentAttempt attempt = paymentAttemptRepository.save(PaymentAttempt.requested(
                payment,
                PaymentProviderType.TOSS,
                request.amount(),
                requestedAt
        ));

        try {
            TossPaymentResponse tossResponse = tossPaymentClient.confirm(new TossConfirmPaymentRequest(
                    request.paymentKey(),
                    order.getOrderNumber(),
                    request.amount()
            ));
            return completeTossPayment(userId, order, orderItems, payment, attempt, tossResponse);
        } catch (TossPaymentException e) {
            return failTossPayment(order, payment, attempt, request, e);
        }
    }

    @Transactional
    public PaymentResponse failPayment(Long userId, Long orderId, FailPaymentRequest request) {
        Order order = findMyPaymentOrder(userId, orderId);
        validateFailableOrder(order);
        validatePaymentAmount(order, request.amount());

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
        return orderRepository.findByIdAndUserId(orderId, userId)
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

    private void validatePaymentAmount(Order order, int requestAmount) {
        if (order.getFinalPrice() != requestAmount) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void validateDuplicateEnrollment(Long userId, List<OrderItem> orderItems) {
        List<Long> courseIds = orderItems.stream()
                .map(OrderItem::getCourseId)
                .distinct()
                .toList();

        List<Long> activeCourseIds = enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                userId,
                EnrollmentStatus.ACTIVE,
                courseIds
        );
        if (!activeCourseIds.isEmpty()) {
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

    private Payment processDeclinedPayment(Order order, FailPaymentRequest request, LocalDateTime declinedAt) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseGet(() -> Payment.createReady(order, declinedAt));
        payment.markProcessing(declinedAt);
        payment.decline(request.paymentKey(), request.method(), request.failedReason(), declinedAt);
        return paymentRepository.save(payment);
    }

    private Payment processTossPaymentRequested(
            Order order,
            Optional<Payment> existingPayment,
            LocalDateTime requestedAt
    ) {
        Payment payment = existingPayment.orElseGet(() -> Payment.createReady(order, PaymentProviderType.TOSS, requestedAt));
        payment.markProcessing(PaymentProviderType.TOSS, requestedAt);
        return paymentRepository.save(payment);
    }

    private ConfirmPaymentResponse completeTossPayment(
            Long userId,
            Order order,
            List<OrderItem> orderItems,
            Payment payment,
            PaymentAttempt attempt,
            TossPaymentResponse tossResponse
    ) {
        LocalDateTime completedAt = approvedAtOrNow(tossResponse.approvedAt());
        if (!isDoneTossPayment(tossResponse, order)) {
            attempt.decline("TOSS_NOT_DONE", "Toss Payments 승인 결과가 완료 상태가 아닙니다.", completedAt);
            payment.decline(
                    tossResponse.paymentKey(),
                    tossResponse.method(),
                    "TOSS_NOT_DONE",
                    "Toss Payments 승인 결과가 완료 상태가 아닙니다.",
                    completedAt
            );
            return ConfirmPaymentResponse.from(paymentRepository.save(payment), order, List.of());
        }

        attempt.succeed(tossResponse.paymentKey(), completedAt);
        payment.succeed(tossResponse.paymentKey(), tossResponse.method(), completedAt);
        Payment savedPayment = paymentRepository.save(payment);

        markOrderPaid(order, completedAt);
        List<Enrollment> savedEnrollments = issueEnrollmentsAtPaidTime(userId, order, orderItems, completedAt);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
    }

    private ConfirmPaymentResponse failTossPayment(
            Order order,
            Payment payment,
            PaymentAttempt attempt,
            ConfirmPaymentRequest request,
            TossPaymentException exception
    ) {
        LocalDateTime completedAt = LocalDateTime.now(clock);
        switch (exception.getFailureType()) {
            case DECLINED -> {
                attempt.decline(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
                payment.decline(
                        request.paymentKey(),
                        null,
                        exception.getFailureCode(),
                        exception.getFailureMessage(),
                        completedAt
                );
            }
            case TIMEOUT -> {
                attempt.markTimeout(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
                payment.markUnknown(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
            }
            case UNKNOWN -> {
                attempt.markUnknown(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
                payment.markUnknown(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
            }
        }

        return ConfirmPaymentResponse.from(paymentRepository.save(payment), order, List.of());
    }

    private boolean isDoneTossPayment(TossPaymentResponse response, Order order) {
        return ("DONE".equals(response.status()) || "SUCCESS".equals(response.status()))
                && order.getOrderNumber().equals(response.orderId())
                && order.getFinalPrice() == response.totalAmount();
    }

    private LocalDateTime approvedAtOrNow(OffsetDateTime approvedAt) {
        if (approvedAt == null) {
            return LocalDateTime.now(clock);
        }
        return approvedAt.atZoneSameInstant(clock.getZone()).toLocalDateTime();
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
