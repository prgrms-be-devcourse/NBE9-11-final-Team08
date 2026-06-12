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

    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validatePaymentOrder(order);
        validateDuplicatePayment(orderId);

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime paidAt = LocalDateTime.now();
        Payment payment = Payment.createReady(order.getId(), order.getFinalPrice(), paidAt);
        payment.succeed(createMockPaymentKey(), MOCK_PAYMENT_METHOD, paidAt);
        Payment savedPayment = paymentRepository.save(payment);

        order.markPaid(paidAt);

        List<Enrollment> enrollments = orderItems.stream()
                .map(orderItem -> Enrollment.createActive(userId, orderItem.getCourseId(), order.getId(), paidAt))
                .toList();
        List<Enrollment> savedEnrollments = enrollmentRepository.saveAll(enrollments);

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
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
        if (paymentRepository.existsByOrderId(orderId)) {
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

    private String createMockPaymentKey() {
        return "MOCK-" + UUID.randomUUID();
    }
}
