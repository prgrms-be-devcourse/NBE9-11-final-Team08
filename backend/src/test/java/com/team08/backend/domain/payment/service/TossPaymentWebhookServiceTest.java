package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest.TossPaymentWebhookData;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentRecoveryTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TossPaymentWebhookServiceTest {

    private static final String ORDER_NUMBER = "ORD-20260612100000-ABC12345";
    private static final String PAYMENT_KEY = "payment-key";
    private static final TossPaymentRecoveryTarget TARGET =
            new TossPaymentRecoveryTarget(100L, 10L, 1L, ORDER_NUMBER, 30_000);

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    private TossPaymentWebhookService tossPaymentWebhookService;

    @BeforeEach
    void setUp() {
        tossPaymentWebhookService = new TossPaymentWebhookService(tossPaymentClient, paymentTransactionService);
    }

    @Test
    void unknownPaymentWebhookSuccessDelegatesToRecovery() {
        TossPaymentResponse tossResponse = tossResponse("DONE");

        given(paymentTransactionService.findTossRecoveryTargetByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(TARGET));
        given(tossPaymentClient.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.of(tossResponse));

        tossPaymentWebhookService.handle(paymentStatusChangedWebhook("DONE"));

        verify(tossPaymentClient).findByPaymentKey(PAYMENT_KEY);
        verify(paymentTransactionService).recoverTossPayment(TARGET, tossResponse);
    }

    @Test
    void alreadyRecoveredPaymentWebhookIsHandledIdempotently() {
        TossPaymentResponse tossResponse = tossResponse("DONE");

        given(paymentTransactionService.findTossRecoveryTargetByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(TARGET));
        given(tossPaymentClient.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.of(tossResponse));
        given(paymentTransactionService.recoverTossPayment(TARGET, tossResponse)).willReturn(false);

        tossPaymentWebhookService.handle(paymentStatusChangedWebhook("DONE"));

        verify(paymentTransactionService).recoverTossPayment(TARGET, tossResponse);
        verify(paymentTransactionService, never()).recoverTossPaymentNotFound(TARGET);
    }

    @Test
    void tossLookupFailureDoesNotMarkPaymentSuccess() {
        TossPaymentException exception = TossPaymentException.timeout("TOSS_TIMEOUT", "Toss timeout");

        given(tossPaymentClient.findByPaymentKey(PAYMENT_KEY)).willThrow(exception);

        tossPaymentWebhookService.handle(paymentStatusChangedWebhook("DONE"));

        verify(paymentTransactionService, never()).keepTossPaymentUnknown(TARGET, "TOSS_TIMEOUT", "Toss timeout");
        verify(paymentTransactionService, never()).recoverTossPayment(TARGET, tossResponse("DONE"));
    }

    @Test
    void canceledWebhookDelegatesToRecoveryWithVerifiedTossStatus() {
        TossPaymentResponse tossResponse = tossResponse("CANCELED");

        given(paymentTransactionService.findTossRecoveryTargetByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(TARGET));
        given(tossPaymentClient.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.of(tossResponse));

        tossPaymentWebhookService.handle(paymentStatusChangedWebhook("CANCELED"));

        verify(paymentTransactionService).recoverTossPayment(TARGET, tossResponse);
    }

    @Test
    void unknownEventTypeIsIgnored() {
        TossPaymentWebhookRequest request = new TossPaymentWebhookRequest(
                "UNKNOWN_EVENT",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new TossPaymentWebhookData(PAYMENT_KEY, ORDER_NUMBER, "DONE", 30_000L)
        );

        tossPaymentWebhookService.handle(request);

        verifyNoInteractions(tossPaymentClient, paymentTransactionService);
    }

    @Test
    void webhookWithoutPaymentKeyUsesOrderIdLookup() {
        TossPaymentWebhookRequest request = new TossPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new TossPaymentWebhookData(null, ORDER_NUMBER, "DONE", 30_000L)
        );
        TossPaymentResponse tossResponse = tossResponse("DONE");

        given(paymentTransactionService.findTossRecoveryTargetByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(TARGET));
        given(tossPaymentClient.findByOrderId(ORDER_NUMBER)).willReturn(Optional.of(tossResponse));

        tossPaymentWebhookService.handle(request);

        verify(tossPaymentClient).findByOrderId(ORDER_NUMBER);
        verify(paymentTransactionService).recoverTossPayment(TARGET, tossResponse);
    }

    @Test
    void verifiedTossOrderIdIsUsedWhenPayloadOrderIdDiffers() {
        String payloadOrderNumber = "ORD-WRONG";
        TossPaymentWebhookRequest request = new TossPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new TossPaymentWebhookData(PAYMENT_KEY, payloadOrderNumber, "DONE", 30_000L)
        );
        TossPaymentResponse tossResponse = tossResponse("DONE");

        given(tossPaymentClient.findByPaymentKey(PAYMENT_KEY)).willReturn(Optional.of(tossResponse));
        given(paymentTransactionService.findTossRecoveryTargetByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(TARGET));

        tossPaymentWebhookService.handle(request);

        verify(paymentTransactionService, never()).findTossRecoveryTargetByOrderNumber(payloadOrderNumber);
        verify(paymentTransactionService).recoverTossPayment(TARGET, tossResponse);
    }

    private TossPaymentWebhookRequest paymentStatusChangedWebhook(String status) {
        return new TossPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new TossPaymentWebhookData(PAYMENT_KEY, ORDER_NUMBER, status, 30_000L)
        );
    }

    private TossPaymentResponse tossResponse(String status) {
        return new TossPaymentResponse(
                PAYMENT_KEY,
                ORDER_NUMBER,
                status,
                "CARD",
                30_000,
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }
}
