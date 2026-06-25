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
import com.team08.backend.domain.payment.entity.PaymentStatus;
import com.team08.backend.domain.payment.repository.PaymentAttemptRepository;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private static final String TOSS_PAYMENT_NOT_FOUND = "TOSS_PAYMENT_NOT_FOUND";
    private static final String TOSS_PAYMENT_LOOKUP_UNKNOWN = "TOSS_PAYMENT_LOOKUP_UNKNOWN";
    private static final List<PaymentStatus> RECOVERABLE_PAYMENT_STATUSES = List.of(
            PaymentStatus.PROCESSING,
            PaymentStatus.UNKNOWN
    );

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final IssuedCouponService issuedCouponService;
    private final OrderCouponUsageRepository orderCouponUsageRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TossPaymentRecoveryTarget> findTossRecoveryTargets(LocalDateTime threshold, int limit) {
        return paymentRepository.findRecoverablePayments(
                        PaymentProviderType.TOSS,
                        RECOVERABLE_PAYMENT_STATUSES,
                        threshold,
                        PageRequest.of(0, limit)
                ).stream()
                .map(payment -> new TossPaymentRecoveryTarget(
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getOrder().getUserId(),
                        payment.getOrder().getOrderNumber(),
                        payment.getAmount()
                ))
                .toList();
    }

    @Transactional
    public TossPaymentProcessingContext prepareTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        Order order = findMyPaymentOrderForUpdate(userId, orderId);

        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        Optional<ConfirmPaymentResponse> idempotentResponse = findIdempotentConfirmResponse(order, existingPayment, request.idempotencyKey());
        if (idempotentResponse.isPresent()) {
            return TossPaymentProcessingContext.replay(idempotentResponse.get());
        }
        validateConfirmableOrder(order);
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
                request.idempotencyKey(),
                requestedAt
        ));

        return TossPaymentProcessingContext.requested(
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

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(context.userId(), orderItems);

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

    @Transactional
    public boolean recoverTossPayment(TossPaymentRecoveryTarget target, TossPaymentResponse tossResponse) {
        Order order = findMyPaymentOrderForUpdate(target.userId(), target.orderId());
        Payment payment = findPayment(target.paymentId());
        if (!isRecoverable(payment)) {
            return false;
        }

        LocalDateTime recoveredAt = approvedAtOrNow(tossResponse.approvedAt());
        if (!isValidRecoveredTossPayment(tossResponse, target)) {
            recoverUnknown(payment, TOSS_PAYMENT_MISMATCH, "Toss Payments 조회 응답이 주문 정보와 일치하지 않습니다.", recoveredAt);
            return true;
        }

        if (isDoneTossPayment(tossResponse)) {
            recoverSuccess(order, payment, tossResponse, recoveredAt);
            return true;
        }

        if (isDeclinedTossPayment(tossResponse)) {
            recoverDecline(payment, tossResponse, recoveredAt);
            return true;
        }

        recoverUnknown(payment, TOSS_PAYMENT_LOOKUP_UNKNOWN, "Toss Payments 조회 결과를 확정할 수 없습니다.", recoveredAt);
        return true;
    }

    @Transactional
    public boolean recoverTossPaymentNotFound(TossPaymentRecoveryTarget target) {
        findMyPaymentOrderForUpdate(target.userId(), target.orderId());
        Payment payment = findPayment(target.paymentId());
        if (!isRecoverable(payment)) {
            return false;
        }

        LocalDateTime recoveredAt = LocalDateTime.now(clock);
        payment.recoverReady(TOSS_PAYMENT_NOT_FOUND, "Toss Payments 결제 내역이 없어 재시도 가능한 상태로 복구합니다.", recoveredAt);
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverUnknown(TOSS_PAYMENT_NOT_FOUND, "Toss Payments 결제 내역이 없습니다.", recoveredAt));
        return true;
    }

    @Transactional
    public boolean keepTossPaymentUnknown(TossPaymentRecoveryTarget target, String failureCode, String failureMessage) {
        findMyPaymentOrderForUpdate(target.userId(), target.orderId());
        Payment payment = findPayment(target.paymentId());
        if (!isRecoverable(payment)) {
            return false;
        }

        LocalDateTime recoveredAt = LocalDateTime.now(clock);
        recoverUnknown(payment, failureCode, failureMessage, recoveredAt);
        return true;
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

        List<Long> existingCourseIds = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                courseIds
        );
        if (!existingCourseIds.isEmpty()) {
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

    private Optional<PaymentAttempt> findLatestAttempt(Long paymentId) {
        return paymentAttemptRepository.findFirstByPayment_IdOrderByCreatedAtDesc(paymentId);
    }

    private boolean isDoneTossPayment(TossPaymentResponse response) {
        return "DONE".equals(response.status()) || "SUCCESS".equals(response.status());
    }

    private boolean isDeclinedTossPayment(TossPaymentResponse response) {
        return "CANCELED".equals(response.status())
                || "ABORTED".equals(response.status())
                || "EXPIRED".equals(response.status());
    }

    private boolean isValidTossPaymentResult(TossPaymentResponse response, Order order, int expectedDiscount) {
        return order.getOrderNumber().equals(response.orderId())
                && (order.getFinalPrice() - expectedDiscount) == response.totalAmount();
    }

    private boolean isValidRecoveredTossPayment(TossPaymentResponse response, TossPaymentRecoveryTarget target) {
        return target.orderNumber().equals(response.orderId())
                && target.amount() == response.totalAmount();
    }

    private boolean isRecoverable(Payment payment) {
        return payment.getStatus() == PaymentStatus.PROCESSING || payment.getStatus() == PaymentStatus.UNKNOWN;
    }

    private void recoverSuccess(Order order, Payment payment, TossPaymentResponse tossResponse, LocalDateTime recoveredAt) {
        payment.recoverSucceed(tossResponse.paymentKey(), tossResponse.method(), recoveredAt);
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverSucceed(tossResponse.paymentKey(), recoveredAt));
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            List<OrderItem> orderItems = findOrderItems(order);
            validateDuplicateEnrollment(order.getUserId(), orderItems);
            markOrderPaid(order, recoveredAt);
            issueEnrollmentsAtPaidTime(order.getUserId(), order, orderItems, recoveredAt);
        }
    }

    private void recoverDecline(Payment payment, TossPaymentResponse tossResponse, LocalDateTime recoveredAt) {
        payment.recoverDecline(
                tossResponse.paymentKey(),
                tossResponse.method(),
                TOSS_NOT_DONE,
                "Toss Payments 조회 결과 결제가 완료되지 않았습니다.",
                recoveredAt
        );
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverDecline(TOSS_NOT_DONE, "Toss Payments 조회 결과 결제가 완료되지 않았습니다.", recoveredAt));
        paymentRepository.save(payment);
    }

    private void recoverUnknown(Payment payment, String failureCode, String failureMessage, LocalDateTime recoveredAt) {
        payment.recoverUnknown(failureCode, failureMessage, recoveredAt);
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverUnknown(failureCode, failureMessage, recoveredAt));
        paymentRepository.save(payment);
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
            int expectedDiscount,
            ConfirmPaymentResponse idempotentResponse
    ) {
        public static TossPaymentProcessingContext requested(
                Long userId,
                Long orderId,
                String orderNumber,
                int amount,
                Long paymentId,
                Long attemptId,
                Long issuedCouponId,
                int expectedDiscount
        ) {
            return new TossPaymentProcessingContext(
                    userId,
                    orderId,
                    orderNumber,
                    amount,
                    paymentId,
                    attemptId,
                    issuedCouponId,
                    expectedDiscount,
                    null
            );
        }

        public static TossPaymentProcessingContext replay(ConfirmPaymentResponse response) {
            return new TossPaymentProcessingContext(null, null, null, 0, null, null, null, 0, response);
        }

        public boolean isIdempotentReplay() {
            return idempotentResponse != null;
        }
    }

    public record TossPaymentRecoveryTarget(
            Long paymentId,
            Long orderId,
            Long userId,
            String orderNumber,
            int amount
    ) {
    }
}
