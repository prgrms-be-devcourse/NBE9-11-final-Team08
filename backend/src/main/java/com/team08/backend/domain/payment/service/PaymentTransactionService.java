package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.ordercouponusage.entity.OrderCouponUsage;
import com.team08.backend.domain.ordercouponusage.repository.OrderCouponUsageRepository;
import com.team08.backend.domain.orderitem.entity.OrderItem;
import com.team08.backend.domain.orderitem.repository.OrderItemRepository;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
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
public class PaymentTransactionService {

    private static final String TOSS_PAYMENT_MISMATCH = "TOSS_PAYMENT_MISMATCH";
    private static final String TOSS_NOT_DONE = "TOSS_NOT_DONE";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final IssuedCouponService issuedCouponService;
    private final OrderCouponUsageRepository orderCouponUsageRepository;
    private final Clock clock;

    @Transactional
    public TossPaymentProcessingContext prepareTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findMyPaymentOrderForUpdate(userId, orderId);

        validateConfirmableOrder(order);
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        validateConfirmablePayment(existingPayment);

        int expectedDiscount = calculateExpectedDiscount(userId, request.issuedCouponId(), order);
        validatePaymentAmount(order, request.amount(), expectedDiscount);

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

        return new TossPaymentProcessingContext(
                userId,
                order.getId(),
                order.getOrderNumber(),
                request.amount(),
                payment.getId(),
                attempt.getId(),
                request.issuedCouponId(),
                expectedDiscount
        );
    }

    @Transactional
    public ConfirmPaymentResponse completeTossPayment(TossPaymentProcessingContext context, TossPaymentResponse tossResponse) {
        Order order = findMyPaymentOrderForUpdate(context.userId(), context.orderId());
        validateConfirmableOrder(order);

        Payment payment = findPayment(context.paymentId());
        PaymentAttempt attempt = findAttempt(context.attemptId());
        LocalDateTime completedAt = approvedAtOrNow(tossResponse.approvedAt());

        if (!isValidTossPaymentResult(tossResponse, order, context.expectedDiscount())) {
            attempt.markUnknown(TOSS_PAYMENT_MISMATCH, "Toss payment result does not match order.", completedAt);
            payment.markUnknown(TOSS_PAYMENT_MISMATCH, "Toss payment result does not match order.", completedAt);
            return ConfirmPaymentResponse.from(paymentRepository.save(payment), order, List.of());
        }

        if (!isDoneTossPayment(tossResponse)) {
            attempt.decline(TOSS_NOT_DONE, "Toss payment confirm result is not done.", completedAt);
            payment.decline(
                    tossResponse.paymentKey(),
                    tossResponse.method(),
                    TOSS_NOT_DONE,
                    "Toss payment confirm result is not done.",
                    completedAt
            );
            return ConfirmPaymentResponse.from(paymentRepository.save(payment), order, List.of());
        }

        attempt.succeed(tossResponse.paymentKey(), completedAt);
        payment.succeed(tossResponse.paymentKey(), tossResponse.method(), completedAt);
        Payment savedPayment = paymentRepository.save(payment);

        if (context.issuedCouponId() != null) {
            int actualDiscount = issuedCouponService.useCouponForOrder(
                    context.userId(),
                    context.issuedCouponId(),
                    order.getTotalPrice()
            );
            order.applyDiscount(actualDiscount);
            orderCouponUsageRepository.save(new OrderCouponUsage(order.getId(), context.issuedCouponId(), actualDiscount));
        }

        markOrderPaid(order, completedAt);
        List<OrderItem> orderItems = findOrderItems(order);
        List<Enrollment> savedEnrollments = issueEnrollmentsAtPaidTime(
                context.userId(),
                order,
                orderItems,
                completedAt
        );

        return ConfirmPaymentResponse.from(savedPayment, order, savedEnrollments);
    }

    @Transactional
    public ConfirmPaymentResponse failTossPayment(
            TossPaymentProcessingContext context,
            ConfirmPaymentRequest request,
            TossPaymentException exception
    ) {
        Order order = findMyPaymentOrderForUpdate(context.userId(), context.orderId());
        validateConfirmableOrder(order);

        Payment payment = findPayment(context.paymentId());
        PaymentAttempt attempt = findAttempt(context.attemptId());
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

    private int calculateExpectedDiscount(Long userId, Long issuedCouponId, Order order) {
        if (issuedCouponId == null) {
            return 0;
        }
        return issuedCouponService.calculateExpectedDiscount(userId, issuedCouponId, order.getTotalPrice()).discountAmount();
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

        List<Long> activeCourseIds = enrollmentRepository.findCourseIdsByUserIdAndStatusAndCourseIdIn(
                userId,
                EnrollmentStatus.ACTIVE,
                courseIds
        );
        if (!activeCourseIds.isEmpty()) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
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

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentAttempt findAttempt(Long attemptId) {
        return paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private boolean isDoneTossPayment(TossPaymentResponse response) {
        return "DONE".equals(response.status()) || "SUCCESS".equals(response.status());
    }

    private boolean isValidTossPaymentResult(TossPaymentResponse response, Order order, int expectedDiscount) {
        return order.getOrderNumber().equals(response.orderId())
                && (order.getFinalPrice() - expectedDiscount) == response.totalAmount();
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

    private List<Enrollment> issueEnrollmentsAtPaidTime(
            Long userId,
            Order order,
            List<OrderItem> orderItems,
            LocalDateTime enrolledAt
    ) {
        List<Enrollment> enrollments = orderItems.stream()
                .map(orderItem -> Enrollment.createActive(userId, orderItem.getCourseId(), order, enrolledAt))
                .toList();

        return enrollmentRepository.saveAll(enrollments);
    }

    public record TossPaymentProcessingContext(
            Long userId,
            Long orderId,
            String orderNumber,
            int amount,
            Long paymentId,
            Long attemptId,
            Long issuedCouponId,
            int expectedDiscount
    ) {
    }
}
