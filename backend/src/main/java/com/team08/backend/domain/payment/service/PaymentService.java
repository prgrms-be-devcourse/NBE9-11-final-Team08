package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String MOCK_PAYMENT_METHOD = "MOCK";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long userId, Long orderId) {
        Order order = findPaymentOrder(userId, orderId);

        // 주문 상태와 Payment 존재 여부를 함께 확인해 중복 결제를 방어한다.
        validatePaymentOrder(order);
        validateDuplicatePayment(orderId);

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Payment savedPayment = createSuccessfulMockPayment(order, paidAt);

        order.markPaid(paidAt);

        // 결제 완료 시점과 수강권 발급 시점을 동일하게 맞춘다.
        List<Enrollment> savedEnrollments = issueEnrollments(userId, order, orderItems, paidAt);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
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

    private void validateDuplicatePayment(Long orderId) {
        if (paymentRepository.existsByOrder_Id(orderId)) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
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

    private Payment createSuccessfulMockPayment(Order order, LocalDateTime paidAt) {
        Payment payment = Payment.createReady(order, paidAt);
        payment.succeed(createMockPaymentKey(), MOCK_PAYMENT_METHOD, paidAt);
        return paymentRepository.save(payment);
    }

    private List<Enrollment> issueEnrollments(Long userId, Order order, List<OrderItem> orderItems, LocalDateTime enrolledAt) {
        List<Enrollment> enrollments = orderItems.stream()
                .map(orderItem -> Enrollment.createActive(userId, orderItem.getCourseId(), order, enrolledAt))
                .toList();

        return enrollmentRepository.saveAll(enrollments);
    }

    private String createMockPaymentKey() {
        return "MOCK-" + UUID.randomUUID();
    }
}
