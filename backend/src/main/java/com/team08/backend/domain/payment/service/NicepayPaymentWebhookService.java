package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentWebhookRequest;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.service.PaymentTransactionService.PaymentRecoveryTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicepayPaymentWebhookService {

    private final PaymentProviderRouter paymentProviderRouter;
    private final PaymentTransactionService paymentTransactionService;

    public NicepayPaymentWebhookResult handle(NicepayPaymentWebhookRequest request) {
        if (request == null) {
            log.info("Ignoring NICEPAY webhook without payload.");
            return NicepayPaymentWebhookResult.IGNORED;
        }

        String paymentKey = request.resolvedPaymentKey();
        String orderId = request.resolvedOrderId();
        if (!StringUtils.hasText(paymentKey) && !StringUtils.hasText(orderId)) {
            log.warn("Ignoring NICEPAY webhook without payment identifier. eventType={}", request.eventType());
            return NicepayPaymentWebhookResult.IGNORED;
        }

        try {
            Optional<PaymentProviderLookupResponse> providerPayment = paymentProviderRouter.lookup(
                    PaymentProviderType.NICEPAY,
                    new PaymentProviderLookupRequest(paymentKey, orderId)
            );
            if (providerPayment.isEmpty()) {
                log.warn("NICEPAY webhook lookup returned empty. orderId={}, paymentKey={}", orderId, paymentKey);
                return NicepayPaymentWebhookResult.IGNORED;
            }

            return recoverWithVerifiedPayment(orderId, providerPayment.get());
        } catch (PaymentProviderException e) {
            log.warn(
                    "NICEPAY webhook lookup failed. orderId={}, paymentKey={}, failureCode={}",
                    orderId,
                    paymentKey,
                    e.getFailureCode()
            );
            return NicepayPaymentWebhookResult.RETRYABLE_FAILURE;
        }
    }

    private NicepayPaymentWebhookResult recoverWithVerifiedPayment(
            String payloadOrderId,
            PaymentProviderLookupResponse providerPayment
    ) {
        Optional<PaymentRecoveryTarget> target = findTarget(providerPayment.orderId());
        if (target.isEmpty()) {
            log.warn(
                    "NICEPAY webhook verified payment but local payment was not found. payloadOrderId={}, nicepayOrderId={}",
                    payloadOrderId,
                    providerPayment.orderId()
            );
            return NicepayPaymentWebhookResult.IGNORED;
        }

        paymentTransactionService.recoverProviderPayment(target.get(), providerPayment);
        return NicepayPaymentWebhookResult.PROCESSED;
    }

    private Optional<PaymentRecoveryTarget> findTarget(String orderNumber) {
        if (!StringUtils.hasText(orderNumber)) {
            return Optional.empty();
        }
        return paymentTransactionService.findProviderRecoveryTargetByOrderNumber(PaymentProviderType.NICEPAY, orderNumber);
    }

    public enum NicepayPaymentWebhookResult {
        PROCESSED,
        IGNORED,
        RETRYABLE_FAILURE
    }
}
