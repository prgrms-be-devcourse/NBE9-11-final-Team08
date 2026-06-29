package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentWebhookRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentWebhookRequest.NicepayPaymentWebhookData;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.service.NicepayPaymentWebhookService.NicepayPaymentWebhookResult;
import com.team08.backend.domain.payment.service.PaymentTransactionService.PaymentRecoveryTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NicepayPaymentWebhookServiceTest {

    private PaymentProviderRouter paymentProviderRouter;
    private PaymentTransactionService paymentTransactionService;
    private NicepayPaymentWebhookService nicepayPaymentWebhookService;

    @BeforeEach
    void setUp() {
        paymentProviderRouter = mock(PaymentProviderRouter.class);
        paymentTransactionService = mock(PaymentTransactionService.class);
        nicepayPaymentWebhookService = new NicepayPaymentWebhookService(paymentProviderRouter, paymentTransactionService);
    }

    @Test
    void verifiedNicepayWebhookRecoversPaymentWithProviderLookupResult() {
        NicepayPaymentWebhookRequest request = webhookRequest("payment-key", "ORD-1");
        PaymentProviderLookupResponse response = providerResponse("ORD-1", "DONE");
        PaymentRecoveryTarget target = recoveryTarget();

        given(paymentProviderRouter.lookup(
                PaymentProviderType.NICEPAY,
                new PaymentProviderLookupRequest("payment-key", "ORD-1")
        )).willReturn(Optional.of(response));
        given(paymentTransactionService.findProviderRecoveryTargetByOrderNumber(PaymentProviderType.NICEPAY, "ORD-1"))
                .willReturn(Optional.of(target));

        NicepayPaymentWebhookResult result = nicepayPaymentWebhookService.handle(request);

        assertThat(result).isEqualTo(NicepayPaymentWebhookResult.PROCESSED);
        verify(paymentTransactionService).recoverProviderPayment(target, response);
    }

    @Test
    void lookupFailureReturnsRetryableFailureWithoutRecoveringFromPayloadOnly() {
        NicepayPaymentWebhookRequest request = webhookRequest("payment-key", "ORD-1");
        PaymentProviderException exception = PaymentProviderException.timeout("NICEPAY_TIMEOUT", "NICEPAY timeout");

        given(paymentProviderRouter.lookup(
                PaymentProviderType.NICEPAY,
                new PaymentProviderLookupRequest("payment-key", "ORD-1")
        )).willThrow(exception);

        NicepayPaymentWebhookResult result = nicepayPaymentWebhookService.handle(request);

        assertThat(result).isEqualTo(NicepayPaymentWebhookResult.RETRYABLE_FAILURE);
        verify(paymentTransactionService, never()).recoverProviderPayment(any(), any());
    }

    @Test
    void webhookWithoutIdentifierIsIgnored() {
        NicepayPaymentWebhookRequest request = new NicepayPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                null,
                null,
                null,
                null,
                "DONE",
                30_000L
        );

        NicepayPaymentWebhookResult result = nicepayPaymentWebhookService.handle(request);

        assertThat(result).isEqualTo(NicepayPaymentWebhookResult.IGNORED);
        verifyNoInteractions(paymentProviderRouter);
    }

    private NicepayPaymentWebhookRequest webhookRequest(String paymentKey, String orderId) {
        return new NicepayPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new NicepayPaymentWebhookData(paymentKey, null, orderId, "DONE", 30_000L),
                null,
                null,
                null,
                null,
                null
        );
    }

    private PaymentProviderLookupResponse providerResponse(String orderNumber, String status) {
        return new PaymentProviderLookupResponse(
                "payment-key",
                orderNumber,
                status,
                "CARD",
                30_000,
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }

    private PaymentRecoveryTarget recoveryTarget() {
        return new PaymentRecoveryTarget(PaymentProviderType.NICEPAY, 100L, 10L, 1L, "ORD-1", 30_000);
    }
}
