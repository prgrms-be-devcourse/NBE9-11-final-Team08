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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findPaymentOrder(userId, orderId);

        // 주문 상태와 Payment 존재 여부를 함께 확인해 중복 결제를 방어한다.
        validatePaymentOrder(order);
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        validateConfirmablePayment(existingPayment);
        validatePaymentAmount(order, request.amount());

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Payment savedPayment = createSuccessfulPayment(order, existingPayment, request, paidAt);

        order.markPaid(paidAt);

        // 결제 완료 시점과 수강권 발급 시점을 동일하게 맞춘다.
        List<Enrollment> savedEnrollments = issueEnrollments(userId, order, orderItems, paidAt);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
    }

    @Transactional
    public PaymentResponse failPayment(Long userId, Long orderId, FailPaymentRequest request) {
        Order order = findPaymentOrder(userId, orderId);
        validatePaymentFailureOrder(order);
        validatePaymentAmount(order, request.amount());

        LocalDateTime failedAt = LocalDateTime.now(clock);
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> Payment.createReady(order, failedAt));
        payment.fail(request.paymentKey(), request.method(), request.failedReason(), failedAt);

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.from(savedPayment, order);
    }

    @Transactional
    public PaymentResponse refundPayment(Long userId, Long orderId) {
        Order order = findPaymentOrder(userId, orderId);
        validateRefundableOrder(order);
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        LocalDateTime refundedAt = LocalDateTime.now(clock);
        payment.refund(refundedAt);
        order.refund(refundedAt);
        cancelActiveEnrollments(order, refundedAt);

        return PaymentResponse.from(payment, order);
    }

    private Order findPaymentOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private List<OrderItem> findOrderItems(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId());
    }

    private void validatePaymentOrder(Order order) {
        if (order.getStatus() == OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_ORDER_STATUS);
        }
    }

    private void validateConfirmablePayment(Optional<Payment> payment) {
        if (payment.isPresent() && payment.get().getStatus() == PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }
    }

    private void validatePaymentFailureOrder(Order order) {
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
        boolean hasActiveEnrollment = orderItems.stream()
                .anyMatch(orderItem -> enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                        userId,
                        orderItem.getCourseId(),
                        EnrollmentStatus.ACTIVE
                ));

        if (hasActiveEnrollment) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
    }

    private Payment createSuccessfulPayment(
            Order order,
            Optional<Payment> existingPayment,
            ConfirmPaymentRequest request,
            LocalDateTime paidAt
    ) {
        Payment payment = existingPayment.orElseGet(() -> Payment.createReady(order, paidAt));
        payment.succeed(request.paymentKey(), request.method(), paidAt);
        return paymentRepository.save(payment);
    }

    private List<Enrollment> issueEnrollments(Long userId, Order order, List<OrderItem> orderItems, LocalDateTime enrolledAt) {
        List<Enrollment> enrollments = orderItems.stream()
                .map(orderItem -> Enrollment.createActive(userId, orderItem.getCourseId(), order, enrolledAt))
                .toList();

        return enrollmentRepository.saveAll(enrollments);
    }

    private void cancelActiveEnrollments(Order order, LocalDateTime canceledAt) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByOrder_IdAndStatus(order.getId(), EnrollmentStatus.ACTIVE);
        enrollments.forEach(enrollment -> enrollment.cancel(canceledAt));
    }
}
