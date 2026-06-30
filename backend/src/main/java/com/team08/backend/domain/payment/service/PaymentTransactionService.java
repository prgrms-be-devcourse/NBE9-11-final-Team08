package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.issuedcoupon.dto.CouponUsageResult;
import com.team08.backend.domain.issuedcoupon.service.IssuedCouponService;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.entity.OrderStatus;
import com.team08.backend.domain.order.repository.OrderRepository;
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
import com.team08.backend.domain.payment.outbox.PaymentSuccessOutboxService;
import com.team08.backend.domain.payment.provider.PaymentProviderConfirmResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupResponse;
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

    private static final String RECOVERY_DUPLICATE_ENROLLMENT = "RECOVERY_DUPLICATE_ENROLLMENT";
    private static final String TOSS_RECOVERY_DUPLICATE_ENROLLMENT_MESSAGE =
            "Toss 결제는 성공으로 조회됐지만 이미 수강권이 있어 자동 복구를 완료할 수 없습니다.";
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
    private final PaymentSuccessOutboxService paymentSuccessOutboxService;
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

    @Transactional(readOnly = true)
    public Optional<TossPaymentRecoveryTarget> findTossRecoveryTargetByOrderNumber(String orderNumber) {
        return paymentRepository.findByProviderAndOrder_OrderNumber(PaymentProviderType.TOSS, orderNumber)
                .map(payment -> new TossPaymentRecoveryTarget(
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getOrder().getUserId(),
                        payment.getOrder().getOrderNumber(),
                        payment.getAmount()
                ));
    }

    @Transactional(readOnly = true)
    public List<PaymentRecoveryTarget> findProviderRecoveryTargets(LocalDateTime threshold, int limit) {
        return paymentRepository.findRecoverablePayments(
                        RECOVERABLE_PAYMENT_STATUSES,
                        threshold,
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toRecoveryTarget)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PaymentRecoveryTarget> findProviderRecoveryTargetByOrderNumber(
            PaymentProviderType providerType,
            String orderNumber
    ) {
        return paymentRepository.findByProviderAndOrder_OrderNumber(providerType, orderNumber)
                .map(this::toRecoveryTarget);
    }

    @Transactional
    public TossPaymentProcessingContext prepareTossPayment(Long userId, Long orderId, ConfirmPaymentRequest request) {
        return prepareProviderPayment(userId, orderId, PaymentProviderType.TOSS, request);
    }

    @Transactional
    public TossPaymentProcessingContext prepareProviderPayment(
            Long userId,
            Long orderId,
            PaymentProviderType providerType,
            ConfirmPaymentRequest request
    ) {
        Order order = findMyPaymentOrderForUpdate(userId, orderId);

        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        Optional<ConfirmPaymentResponse> idempotentResponse = findIdempotentConfirmResponse(order, existingPayment, request.idempotencyKey());
        if (idempotentResponse.isPresent()) {
            return TossPaymentProcessingContext.replay(idempotentResponse.get());
        }
        validateConfirmableOrder(order);
        validateConfirmablePayment(existingPayment);

        int expectedDiscount = calculateExpectedDiscounts(userId, request.itemCouponIds(), request.stackableCouponId(), order, findOrderItems(order));
        validatePaymentAmount(order, request.amount(), expectedDiscount);

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(userId, orderItems);

        LocalDateTime requestedAt = LocalDateTime.now(clock);
        Payment payment = processProviderPaymentRequested(order, existingPayment, providerType, requestedAt);
        PaymentAttempt attempt = paymentAttemptRepository.save(PaymentAttempt.requested(
                payment,
                providerType,
                request.amount(),
                request.idempotencyKey(),
                requestedAt
        ));

        return TossPaymentProcessingContext.requested(
                providerType,
                userId,
                order.getId(),
                order.getOrderNumber(),
                request.amount(),
                payment.getId(),
                attempt.getId(),
                request.itemCouponIds(),
                request.stackableCouponId(),
                expectedDiscount
        );
    }

    @Transactional
    public ConfirmPaymentResponse completeTossPayment(TossPaymentProcessingContext context, TossPaymentResponse tossResponse) {
        return completeProviderPayment(context, toProviderResponse(tossResponse));
    }

    @Transactional
    public ConfirmPaymentResponse completeProviderPayment(
            TossPaymentProcessingContext context,
            PaymentProviderConfirmResponse providerResponse
    ) {
        Order order = findMyPaymentOrderForUpdate(context.userId(), context.orderId());
        validateConfirmableOrder(order);

        Payment payment = findPayment(context.paymentId());
        PaymentAttempt attempt = findAttempt(context.attemptId());
        LocalDateTime completedAt = approvedAtOrNow(providerResponse.approvedAt());

        if (!isValidProviderPaymentResult(providerResponse, order, context.expectedDiscount())) {
            String mismatchCode = providerMismatchCode(context.providerType(), providerResponse, order, context.expectedDiscount());
            attempt.markUnknown(mismatchCode, "Provider payment result does not match order.", completedAt);
            payment.markUnknown(
                    providerResponse.paymentKey(),
                    providerResponse.method(),
                    mismatchCode,
                    "Provider payment result does not match order.",
                    completedAt
            );
            return ConfirmPaymentResponse.from(paymentRepository.save(payment), order);
        }

        if (!isDoneProviderPayment(providerResponse)) {
            attempt.decline(providerNotDoneCode(context.providerType()), "Provider payment confirm result is not done.", completedAt);
            payment.decline(
                    providerResponse.paymentKey(),
                    providerResponse.method(),
                    providerNotDoneCode(context.providerType()),
                    "Provider payment confirm result is not done.",
                    completedAt
            );
            return ConfirmPaymentResponse.from(paymentRepository.save(payment), order);
        }

        List<OrderItem> orderItems = findOrderItems(order);
        validateDuplicateEnrollment(context.userId(), orderItems);

        attempt.succeed(providerResponse.paymentKey(), completedAt);
        payment.succeed(providerResponse.paymentKey(), providerResponse.method(), completedAt);
        Payment savedPayment = paymentRepository.save(payment);

        if ((context.itemCouponIds() != null && !context.itemCouponIds().isEmpty()) || context.stackableCouponId() != null) {
            CouponUsageResult usageResult = issuedCouponService.useCouponsForOrder(
                    context.userId(),
                    context.itemCouponIds(),
                    context.stackableCouponId(),
                    orderItems,
                    order.getTotalPrice(),
                    order.getId()
            );
            order.applyDiscount(usageResult.totalDiscount());
            orderCouponUsageRepository.saveAll(usageResult.usages());
        }

        markOrderPaid(order, completedAt);
        paymentSuccessOutboxService.createIfAbsent(savedPayment.getId(), order.getId(), context.userId());

        return ConfirmPaymentResponse.from(savedPayment, order);
    }

    @Transactional
    public ConfirmPaymentResponse failTossPayment(
            TossPaymentProcessingContext context,
            ConfirmPaymentRequest request,
            TossPaymentException exception
    ) {
        return failProviderPayment(context, request, toProviderException(exception));
    }

    @Transactional
    public ConfirmPaymentResponse failProviderPayment(
            TossPaymentProcessingContext context,
            ConfirmPaymentRequest request,
            PaymentProviderException exception
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
                        providerPaymentKey(request),
                        providerPaymentMethod(request),
                        exception.getFailureCode(),
                        exception.getFailureMessage(),
                        completedAt
                );
            }
            case TIMEOUT -> {
                attempt.markTimeout(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
                payment.markUnknown(
                        providerPaymentKey(request),
                        providerPaymentMethod(request),
                        exception.getFailureCode(),
                        exception.getFailureMessage(),
                        completedAt
                );
            }
            case UNKNOWN -> {
                attempt.markUnknown(exception.getFailureCode(), exception.getFailureMessage(), completedAt);
                payment.markUnknown(
                        providerPaymentKey(request),
                        providerPaymentMethod(request),
                        exception.getFailureCode(),
                        exception.getFailureMessage(),
                        completedAt
                );
            }
        }

        return ConfirmPaymentResponse.from(paymentRepository.save(payment), order);
    }

    @Transactional
    public boolean recoverTossPayment(TossPaymentRecoveryTarget target, TossPaymentResponse tossResponse) {
        return recoverProviderPayment(target.toProviderTarget(), toProviderLookupResponse(tossResponse));
    }

    @Transactional
    public boolean recoverProviderPayment(
            PaymentRecoveryTarget target,
            PaymentProviderLookupResponse providerResponse
    ) {
        Order order = findMyPaymentOrderForUpdate(target.userId(), target.orderId());
        Payment payment = findPayment(target.paymentId());
        if (!isRecoverable(payment)) {
            return false;
        }

        LocalDateTime recoveredAt = approvedAtOrNow(providerResponse.approvedAt());
        if (!isValidRecoveredProviderPayment(providerResponse, target)) {
            recoverUnknown(payment, providerMismatchCode(target.providerType()), "Provider lookup result does not match order.", recoveredAt);
            return true;
        }

        if (isDoneProviderPayment(providerResponse)) {
            recoverSuccess(order, payment, providerResponse, recoveredAt);
            return true;
        }

        if (isDeclinedProviderPayment(providerResponse)) {
            recoverDecline(payment, providerResponse, recoveredAt);
            return true;
        }

        recoverUnknown(payment, providerLookupUnknownCode(target.providerType()), "Provider lookup result is unclear.", recoveredAt);
        return true;
    }

    @Transactional
    public boolean recoverTossPaymentNotFound(TossPaymentRecoveryTarget target) {
        return recoverProviderPaymentNotFound(target.toProviderTarget());
    }

    @Transactional
    public boolean recoverProviderPaymentNotFound(PaymentRecoveryTarget target) {
        findMyPaymentOrderForUpdate(target.userId(), target.orderId());
        Payment payment = findPayment(target.paymentId());
        if (!isRecoverable(payment)) {
            return false;
        }

        LocalDateTime recoveredAt = LocalDateTime.now(clock);
        String failureCode = providerPaymentNotFoundCode(target.providerType());
        payment.recoverReady(failureCode, "Provider payment was not found. Payment can be retried.", recoveredAt);
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverUnknown(failureCode, "Provider payment was not found.", recoveredAt));
        return true;
    }

    @Transactional
    public boolean keepTossPaymentUnknown(TossPaymentRecoveryTarget target, String failureCode, String failureMessage) {
        return keepProviderPaymentUnknown(target.toProviderTarget(), failureCode, failureMessage);
    }

    @Transactional
    public boolean keepProviderPaymentUnknown(PaymentRecoveryTarget target, String failureCode, String failureMessage) {
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
        return ConfirmPaymentResponse.from(payment, order);
    }

    private int calculateExpectedDiscounts(Long userId, java.util.Map<Long, Long> itemCouponIds, Long stackableCouponId, Order order, List<OrderItem> orderItems) {
        if ((itemCouponIds == null || itemCouponIds.isEmpty()) && stackableCouponId == null) {
            return 0;
        }
        return issuedCouponService.calculateExpectedDiscounts(userId, itemCouponIds, stackableCouponId, orderItems, order.getTotalPrice());
    }

    private void validatePaymentAmount(Order order, int requestAmount, int expectedDiscount) {
        if (order.getFinalPrice() - expectedDiscount != requestAmount) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void validateDuplicateEnrollment(Long userId, List<OrderItem> orderItems) {
        if (hasDuplicateEnrollment(userId, orderItems)) {
            throw new CustomException(ErrorCode.LECTURE_ALREADY_ENROLLED);
        }
    }

    private boolean hasDuplicateEnrollment(Long userId, List<OrderItem> orderItems) {
        List<Long> courseIds = orderItems.stream()
                .map(OrderItem::getCourseId)
                .distinct()
                .toList();

        List<Long> existingCourseIds = enrollmentRepository.findCourseIdsByUserIdAndCourseIdIn(
                userId,
                courseIds
        );
        return !existingCourseIds.isEmpty();
    }

    private Payment processProviderPaymentRequested(
            Order order,
            Optional<Payment> existingPayment,
            PaymentProviderType providerType,
            LocalDateTime requestedAt
    ) {
        Payment payment = existingPayment.orElseGet(() -> Payment.createReady(order, providerType, requestedAt));
        payment.markProcessing(providerType, requestedAt);
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

    private boolean isDoneProviderPayment(PaymentProviderConfirmResponse response) {
        return "DONE".equals(response.status()) || "SUCCESS".equals(response.status());
    }

    private boolean isDoneProviderPayment(PaymentProviderLookupResponse response) {
        return "DONE".equals(response.status()) || "SUCCESS".equals(response.status());
    }

    private boolean isDeclinedProviderPayment(PaymentProviderLookupResponse response) {
        return "CANCELED".equals(response.status())
                || "CANCELLED".equals(response.status())
                || "ABORTED".equals(response.status())
                || "EXPIRED".equals(response.status())
                || "FAILED".equals(response.status())
                || "DECLINED".equals(response.status());
    }

    private boolean isValidTossPaymentResult(TossPaymentResponse response, Order order, int expectedDiscount) {
        return order.getOrderNumber().equals(response.orderId())
                && (order.getFinalPrice() - expectedDiscount) == response.totalAmount();
    }

    private boolean isValidProviderPaymentResult(PaymentProviderConfirmResponse response, Order order, int expectedDiscount) {
        return order.getOrderNumber().equals(response.orderId())
                && (order.getFinalPrice() - expectedDiscount) == response.totalAmount();
    }

    private boolean isValidRecoveredProviderPayment(PaymentProviderLookupResponse response, PaymentRecoveryTarget target) {
        return target.orderNumber().equals(response.orderId())
                && target.amount() == response.totalAmount();
    }

    private boolean isRecoverable(Payment payment) {
        return payment.getStatus() == PaymentStatus.PROCESSING || payment.getStatus() == PaymentStatus.UNKNOWN;
    }

    private void recoverSuccess(Order order, Payment payment, TossPaymentResponse tossResponse, LocalDateTime recoveredAt) {
        recoverSuccess(order, payment, toProviderLookupResponse(tossResponse), recoveredAt);
    }

    private void recoverSuccess(Order order, Payment payment, PaymentProviderLookupResponse providerResponse, LocalDateTime recoveredAt) {
        List<OrderItem> orderItems = List.of();
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderItems = findOrderItems(order);
            if (hasDuplicateEnrollment(order.getUserId(), orderItems)) {
                recoverUnknown(
                        payment,
                        providerRecoveryDuplicateEnrollmentCode(payment.getProvider()),
                        TOSS_RECOVERY_DUPLICATE_ENROLLMENT_MESSAGE,
                        recoveredAt
                );
                return;
            }
        }

        payment.recoverSucceed(providerResponse.paymentKey(), providerResponse.method(), recoveredAt);
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverSucceed(providerResponse.paymentKey(), recoveredAt));
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            markOrderPaid(order, recoveredAt);
            paymentSuccessOutboxService.createIfAbsent(payment.getId(), order.getId(), order.getUserId());
        }
    }

    private void recoverDecline(Payment payment, TossPaymentResponse tossResponse, LocalDateTime recoveredAt) {
        recoverDecline(payment, toProviderLookupResponse(tossResponse), recoveredAt);
    }

    private void recoverDecline(Payment payment, PaymentProviderLookupResponse providerResponse, LocalDateTime recoveredAt) {
        String failureCode = providerNotDoneCode(payment.getProvider());
        String failureMessage = "Provider lookup result is not done.";
        payment.recoverDecline(
                providerResponse.paymentKey(),
                providerResponse.method(),
                failureCode,
                failureMessage,
                recoveredAt
        );
        findLatestAttempt(payment.getId())
                .ifPresent(attempt -> attempt.recoverDecline(failureCode, failureMessage, recoveredAt));
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

    private PaymentProviderConfirmResponse toProviderResponse(TossPaymentResponse tossResponse) {
        return new PaymentProviderConfirmResponse(
                tossResponse.paymentKey(),
                tossResponse.orderId(),
                tossResponse.status(),
                tossResponse.method(),
                tossResponse.totalAmount(),
                tossResponse.approvedAt()
        );
    }

    private PaymentProviderLookupResponse toProviderLookupResponse(TossPaymentResponse tossResponse) {
        return new PaymentProviderLookupResponse(
                tossResponse.paymentKey(),
                tossResponse.orderId(),
                tossResponse.status(),
                tossResponse.method(),
                tossResponse.totalAmount(),
                tossResponse.approvedAt()
        );
    }

    private PaymentRecoveryTarget toRecoveryTarget(Payment payment) {
        return new PaymentRecoveryTarget(
                payment.getProvider(),
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getUserId(),
                payment.getOrder().getOrderNumber(),
                payment.getAmount()
        );
    }

    private PaymentProviderException toProviderException(TossPaymentException exception) {
        return switch (exception.getFailureType()) {
            case DECLINED ->
                    PaymentProviderException.declined(exception.getFailureCode(), exception.getFailureMessage());
            case TIMEOUT -> PaymentProviderException.timeout(exception.getFailureCode(), exception.getFailureMessage());
            case UNKNOWN -> PaymentProviderException.unknown(exception.getFailureCode(), exception.getFailureMessage());
        };
    }

    private String providerMismatchCode(PaymentProviderType providerType) {
        return providerType.name() + "_PAYMENT_MISMATCH";
    }

    private String providerMismatchCode(
            PaymentProviderType providerType,
            PaymentProviderConfirmResponse response,
            Order order,
            int expectedDiscount
    ) {
        if (!order.getOrderNumber().equals(response.orderId())) {
            return providerType.name() + "_ORDER_MISMATCH";
        }
        if ((order.getFinalPrice() - expectedDiscount) != response.totalAmount()) {
            return providerType.name() + "_AMOUNT_MISMATCH";
        }
        return providerMismatchCode(providerType);
    }

    private String providerNotDoneCode(PaymentProviderType providerType) {
        return providerType.name() + "_NOT_DONE";
    }

    private String providerPaymentNotFoundCode(PaymentProviderType providerType) {
        return providerType.name() + "_PAYMENT_NOT_FOUND";
    }

    private String providerLookupUnknownCode(PaymentProviderType providerType) {
        return providerType.name() + "_PAYMENT_LOOKUP_UNKNOWN";
    }

    private String providerRecoveryDuplicateEnrollmentCode(PaymentProviderType providerType) {
        return providerType.name() + "_" + RECOVERY_DUPLICATE_ENROLLMENT;
    }

    private String providerPaymentKey(ConfirmPaymentRequest request) {
        return StringUtils.hasText(request.paymentKey()) ? request.paymentKey() : request.txTid();
    }

    private String providerPaymentMethod(ConfirmPaymentRequest request) {
        return StringUtils.hasText(request.method()) ? request.method() : request.payMethod();
    }

    public record TossPaymentProcessingContext(
            PaymentProviderType providerType,
            Long userId,
            Long orderId,
            String orderNumber,
            int amount,
            Long paymentId,
            Long attemptId,
            java.util.Map<Long, Long> itemCouponIds,
            Long stackableCouponId,
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
                java.util.Map<Long, Long> itemCouponIds,
                Long stackableCouponId,
                int expectedDiscount
        ) {
            return requested(
                    PaymentProviderType.TOSS,
                    userId,
                    orderId,
                    orderNumber,
                    amount,
                    paymentId,
                    attemptId,
                    itemCouponIds,
                    stackableCouponId,
                    expectedDiscount
            );
        }

        public static TossPaymentProcessingContext requested(
                PaymentProviderType providerType,
                Long userId,
                Long orderId,
                String orderNumber,
                int amount,
                Long paymentId,
                Long attemptId,
                java.util.Map<Long, Long> itemCouponIds,
                Long stackableCouponId,
                int expectedDiscount
        ) {
            return new TossPaymentProcessingContext(
                    providerType,
                    userId,
                    orderId,
                    orderNumber,
                    amount,
                    paymentId,
                    attemptId,
                    itemCouponIds,
                    stackableCouponId,
                    expectedDiscount,
                    null
            );
        }

        public static TossPaymentProcessingContext replay(ConfirmPaymentResponse response) {
            return new TossPaymentProcessingContext(null, null, null, null, 0, null, null, null, null, 0, response);
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
        private PaymentRecoveryTarget toProviderTarget() {
            return new PaymentRecoveryTarget(
                    PaymentProviderType.TOSS,
                    paymentId,
                    orderId,
                    userId,
                    orderNumber,
                    amount
            );
        }
    }

    public record PaymentRecoveryTarget(
            PaymentProviderType providerType,
            Long paymentId,
            Long orderId,
            Long userId,
            String orderNumber,
            int amount
    ) {
    }
}
