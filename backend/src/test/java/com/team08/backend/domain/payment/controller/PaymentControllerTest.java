package com.team08.backend.domain.payment.controller;

import com.team08.backend.domain.payment.config.TossPaymentProperties;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest.TossPaymentWebhookData;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.domain.payment.service.NicepayPaymentWebhookService;
import com.team08.backend.domain.payment.service.TossPaymentWebhookService;
import com.team08.backend.domain.payment.service.TossPaymentWebhookService.TossPaymentWebhookResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentControllerTest {

    private static final String WEBHOOK_SECRET = "webhook-secret";

    private PaymentService paymentService;
    private TossPaymentWebhookService tossPaymentWebhookService;
    private NicepayPaymentWebhookService nicepayPaymentWebhookService;
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        tossPaymentWebhookService = mock(TossPaymentWebhookService.class);
        nicepayPaymentWebhookService = mock(NicepayPaymentWebhookService.class);
        paymentController = new PaymentController(
                paymentService,
                tossPaymentWebhookService,
                nicepayPaymentWebhookService,
                new TossPaymentProperties(
                        "https://api.tosspayments.com",
                        "test-secret-key",
                        WEBHOOK_SECRET,
                        null,
                        null
                )
        );
    }

    @Test
    void headerSecretAllowsWebhookProcessing() {
        TossPaymentWebhookRequest request = webhookRequest();
        given(tossPaymentWebhookService.handle(request)).willReturn(TossPaymentWebhookResult.PROCESSED);

        ResponseEntity<Void> response = paymentController.handleTossWebhook(WEBHOOK_SECRET, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tossPaymentWebhookService).handle(request);
    }

    @Test
    void queryTokenAllowsWebhookProcessing() {
        TossPaymentWebhookRequest request = webhookRequest();
        given(tossPaymentWebhookService.handle(request)).willReturn(TossPaymentWebhookResult.IGNORED);

        ResponseEntity<Void> response = paymentController.handleTossWebhook(null, WEBHOOK_SECRET, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tossPaymentWebhookService).handle(request);
    }

    @Test
    void wrongSecretRejectsWebhookWithoutProcessing() {
        TossPaymentWebhookRequest request = webhookRequest();

        ResponseEntity<Void> response = paymentController.handleTossWebhook("wrong-secret", null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(tossPaymentWebhookService, never()).handle(request);
    }

    @Test
    void missingConfiguredSecretRejectsWebhookWithoutProcessing() {
        TossPaymentWebhookRequest request = webhookRequest();
        PaymentController controllerWithoutSecret = new PaymentController(
                paymentService,
                tossPaymentWebhookService,
                nicepayPaymentWebhookService,
                new TossPaymentProperties(
                        "https://api.tosspayments.com",
                        "test-secret-key",
                        "",
                        null,
                        null
                )
        );

        ResponseEntity<Void> response = controllerWithoutSecret.handleTossWebhook(WEBHOOK_SECRET, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(tossPaymentWebhookService, never()).handle(request);
    }

    @Test
    void retryableFailureReturnsServiceUnavailable() {
        TossPaymentWebhookRequest request = webhookRequest();
        given(tossPaymentWebhookService.handle(request)).willReturn(TossPaymentWebhookResult.RETRYABLE_FAILURE);

        ResponseEntity<Void> response = paymentController.handleTossWebhook(WEBHOOK_SECRET, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(tossPaymentWebhookService).handle(request);
    }

    private TossPaymentWebhookRequest webhookRequest() {
        return new TossPaymentWebhookRequest(
                "PAYMENT_STATUS_CHANGED",
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00"),
                new TossPaymentWebhookData("payment-key", "ORD-20260612100000-ABC12345", "DONE", 30_000L)
        );
    }
}
