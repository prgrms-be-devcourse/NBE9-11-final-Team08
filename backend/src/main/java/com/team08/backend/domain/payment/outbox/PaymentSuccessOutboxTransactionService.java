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
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.payment.service.PaidCourseStudyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentSuccessOutboxTransactionService {

    private final PaymentSuccessOutboxRepository paymentSuccessOutboxRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaidCourseStudyMemberService paidCourseStudyMemberService;
    private final Clock clock;
    private final PaymentSuccessOutboxProperties properties;

    @Transactional(readOnly = true)
    public List<Long> findReadyIds() {
        LocalDateTime now = LocalDateTime.now(clock);
        return paymentSuccessOutboxRepository.findReady(
                PaymentSuccessOutboxStatus.PENDING,
                PaymentSuccessOutboxStatus.FAILED,
                now,
                PageRequest.of(0, properties.batchSize())
        ).stream().map(PaymentSuccessOutboxEvent::getId).toList();
    }

    @Transactional
    public void processReady(Long eventId) {
        PaymentSuccessOutboxEvent event = paymentSuccessOutboxRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException("결제 성공 후처리 Outbox 이벤트를 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!isReady(event, now)) {
            return;
        }
        event.markProcessing();
        paymentSuccessOutboxRepository.flush();

        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new IllegalStateException("결제 성공 후처리 대상 결제를 찾을 수 없습니다."));
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("결제 성공 후처리 대상 주문을 찾을 수 없습니다."));
        validateTarget(event, payment, order);

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        if (orderItems.isEmpty()) {
            throw new IllegalStateException("결제 성공 후처리 대상 주문 항목이 없습니다.");
        }

        LinkedHashMap<Long, OrderItem> itemsByCourseId = new LinkedHashMap<>();
        orderItems.forEach(item -> itemsByCourseId.putIfAbsent(item.getCourseId(), item));
        List<Long> courseIds = List.copyOf(itemsByCourseId.keySet());

        Set<Long> activeOrderCourseIds = new HashSet<>(
                enrollmentRepository.findAllByOrder_IdAndStatus(order.getId(), EnrollmentStatus.ACTIVE)
                        .stream()
                        .map(Enrollment::getCourseId)
                        .toList()
        );
        Set<Long> existingCourseIds = new HashSet<>(
                enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(event.getUserId(), courseIds)
        );

        Set<Long> conflictingCourseIds = new HashSet<>(existingCourseIds);
        conflictingCourseIds.removeAll(activeOrderCourseIds);
        if (!conflictingCourseIds.isEmpty()) {
            throw new IllegalStateException("다른 주문 또는 비활성 수강권과 중복되어 후처리할 수 없습니다.");
        }

        LocalDateTime enrolledAt = payment.getPaidAt();
        List<Enrollment> newEnrollments = itemsByCourseId.values().stream()
                .filter(item -> !activeOrderCourseIds.contains(item.getCourseId()))
                .map(item -> Enrollment.createActive(event.getUserId(), item.getCourseId(), order, enrolledAt))
                .toList();
        if (!newEnrollments.isEmpty()) {
            enrollmentRepository.saveAllAndFlush(newEnrollments);
        }

        paidCourseStudyMemberService.joinAsMember(event.getUserId(), courseIds, enrolledAt);
        event.markSuccess(LocalDateTime.now(clock));
    }

    @Transactional
    public void markFailed(Long eventId, String errorMessage) {
        PaymentSuccessOutboxEvent event = findEvent(eventId);
        if (event.getStatus() == PaymentSuccessOutboxStatus.PENDING
                || event.getStatus() == PaymentSuccessOutboxStatus.FAILED
                || event.getStatus() == PaymentSuccessOutboxStatus.PROCESSING) {
            LocalDateTime now = LocalDateTime.now(clock);
            event.markFailed(
                    errorMessage,
                    now,
                    properties.maxRetries(),
                    retryDelaySeconds(event.getRetryCount())
            );
        }
    }

    private boolean isReady(PaymentSuccessOutboxEvent event, LocalDateTime now) {
        if (event.getStatus() == PaymentSuccessOutboxStatus.PENDING) {
            return true;
        }

        return event.getStatus() == PaymentSuccessOutboxStatus.FAILED
                && event.getNextRetryAt() != null
                && !event.getNextRetryAt().isAfter(now);
    }

    private long retryDelaySeconds(int retryCount) {
        long multiplier = 1L << Math.min(retryCount, 30);
        long delay = properties.retryBaseDelaySeconds() * multiplier;
        return Math.min(delay, properties.retryMaxDelaySeconds());
    }

    private PaymentSuccessOutboxEvent findEvent(Long eventId) {
        return paymentSuccessOutboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("결제 성공 후처리 Outbox 이벤트를 찾을 수 없습니다."));
    }

    private void validateTarget(PaymentSuccessOutboxEvent event, Payment payment, Order order) {
        if (!payment.getOrder().getId().equals(order.getId())
                || !order.getUserId().equals(event.getUserId())
                || payment.getStatus() != PaymentStatus.SUCCESS
                || order.getStatus() != OrderStatus.PAID
                || payment.getPaidAt() == null) {
            throw new IllegalStateException("결제 성공 후처리 대상의 결제 또는 주문 상태가 올바르지 않습니다.");
        }
    }
}
