package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest.TossPaymentWebhookData;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentRecoveryTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final TossPaymentClient tossPaymentClient;
    private final PaymentTransactionService paymentTransactionService;

    public TossPaymentWebhookResult handle(TossPaymentWebhookRequest request) {
        if (request == null || !PAYMENT_STATUS_CHANGED.equals(request.eventType())) {
            log.info("Ignoring Toss payment webhook event. eventType={}", request == null ? null : request.eventType());
            return TossPaymentWebhookResult.IGNORED;
        }

        TossPaymentWebhookData data = request.data();
        if (data == null || (!StringUtils.hasText(data.paymentKey()) && !StringUtils.hasText(data.orderId()))) {
            log.warn("Ignoring Toss payment webhook without payment identifier. eventType={}", request.eventType());
            return TossPaymentWebhookResult.IGNORED;
        }

        try {
            Optional<TossPaymentResponse> tossPayment = lookupTossPayment(data);
            if (tossPayment.isEmpty()) {
                log.warn("Toss payment webhook lookup returned empty. orderId={}, paymentKey={}", data.orderId(), data.paymentKey());
                return TossPaymentWebhookResult.IGNORED;
            }

            return recoverWithVerifiedTossPayment(data, tossPayment.get());
        } catch (TossPaymentException e) {
            log.warn(
                    "Toss payment webhook lookup failed. orderId={}, paymentKey={}, failureCode={}",
                    data.orderId(),
                    data.paymentKey(),
                    e.getFailureCode()
            );
            return TossPaymentWebhookResult.RETRYABLE_FAILURE;
        }
    }

    private Optional<TossPaymentResponse> lookupTossPayment(TossPaymentWebhookData data) {
        if (StringUtils.hasText(data.paymentKey())) {
            return tossPaymentClient.findByPaymentKey(data.paymentKey());
        }
        return tossPaymentClient.findByOrderId(data.orderId());
    }

    private TossPaymentWebhookResult recoverWithVerifiedTossPayment(
            TossPaymentWebhookData data,
            TossPaymentResponse tossPayment
    ) {
        Optional<TossPaymentRecoveryTarget> target = findTarget(tossPayment.orderId());

        if (target.isEmpty()) {
            log.warn(
                    "Toss payment webhook verified payment but local payment was not found. payloadOrderId={}, tossOrderId={}",
                    data.orderId(),
                    tossPayment.orderId()
            );
            return TossPaymentWebhookResult.IGNORED;
        }

        paymentTransactionService.recoverTossPayment(target.get(), tossPayment);
        return TossPaymentWebhookResult.PROCESSED;
    }

    private Optional<TossPaymentRecoveryTarget> findTarget(String orderNumber) {
        if (!StringUtils.hasText(orderNumber)) {
            return Optional.empty();
        }
        return paymentTransactionService.findTossRecoveryTargetByOrderNumber(orderNumber);
    }

    public enum TossPaymentWebhookResult {
        PROCESSED,
        IGNORED,
        RETRYABLE_FAILURE
    }
}
