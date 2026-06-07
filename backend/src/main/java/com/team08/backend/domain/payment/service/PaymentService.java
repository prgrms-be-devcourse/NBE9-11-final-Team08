package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderItem;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderItemRepository;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Transactional
    public PaymentResponse mockSuccess(Long userId, Long orderId, String paymentKey, String method) {
        Order order = getOrder(userId, orderId);
        Payment payment = getOrCreatePayment(order);

        if (order.getStatus() == OrderStatus.PAID || payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.from(payment);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제할 수 없는 주문 상태입니다.");
        }

        // TODO: 실제 PG 승인 검증은 PG 연동 시 구현합니다.
        payment.succeed(resolvePaymentKey(paymentKey), resolveMethod(method), clock);
        order.markPaid(clock);
        issueEnrollments(order);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse mockFail(Long userId, Long orderId, String failedReason) {
        Order order = getOrder(userId, orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 결제 완료된 주문입니다.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 실패 처리할 수 없는 주문 상태입니다.");
        }

        Payment payment = getOrCreatePayment(order);
        payment.fail(resolveFailedReason(failedReason), clock);

        return PaymentResponse.from(payment);
    }

    private Order getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    private Payment getOrCreatePayment(Order order) {
        return paymentRepository.findByOrderId(order.getId())
                .orElseGet(() -> paymentRepository.save(Payment.ready(order, clock)));
    }

    private void issueEnrollments(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());

        for (OrderItem orderItem : orderItems) {
            boolean alreadyIssued = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                    order.getUser().getId(),
                    orderItem.getCourse().getId(),
                    EnrollmentStatus.ACTIVE
            );
            if (!alreadyIssued) {
                enrollmentRepository.save(Enrollment.active(order.getUser(), orderItem.getCourse(), order, clock));
            }
        }
    }

    private String resolvePaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            return "MOCK-" + System.nanoTime();
        }
        return paymentKey;
    }

    private String resolveMethod(String method) {
        if (method == null || method.isBlank()) {
            return "MOCK";
        }
        return method;
    }

    private String resolveFailedReason(String failedReason) {
        if (failedReason == null || failedReason.isBlank()) {
            return "Mock payment failed";
        }
        return failedReason;
    }
}
