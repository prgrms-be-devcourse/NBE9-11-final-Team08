package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.service.PaymentTransactionService.PaymentRecoveryTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRecoveryServiceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZONE_ID);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private PaymentProviderRouter paymentProviderRouter;

    private PaymentRecoveryService paymentRecoveryService;

    @BeforeEach
    void setUp() {
        paymentRecoveryService = new PaymentRecoveryService(
                paymentTransactionService,
                paymentProviderRouter,
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(paymentRecoveryService, "staleThreshold", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(paymentRecoveryService, "batchSize", 2);
        ReflectionTestUtils.setField(paymentRecoveryService, "recoveryEnabled", true);
    }

    @Test
    void recoversProviderPaymentByBatchLimit() {
        PaymentRecoveryTarget firstTarget = recoveryTarget(PaymentProviderType.TOSS, 100L, "ORD-1");
        PaymentRecoveryTarget secondTarget = recoveryTarget(PaymentProviderType.NICEPAY, 101L, "ORD-2");
        PaymentProviderLookupResponse firstResponse = providerResponse("ORD-1", "DONE");
        PaymentProviderLookupResponse secondResponse = providerResponse("ORD-2", "DONE");

        given(paymentTransactionService.findProviderRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(firstTarget, secondTarget));
        given(paymentProviderRouter.lookup(PaymentProviderType.TOSS, new PaymentProviderLookupRequest(null, "ORD-1")))
                .willReturn(Optional.of(firstResponse));
        given(paymentProviderRouter.lookup(PaymentProviderType.NICEPAY, new PaymentProviderLookupRequest(null, "ORD-2")))
                .willReturn(Optional.of(secondResponse));
        given(paymentTransactionService.recoverProviderPayment(firstTarget, firstResponse)).willReturn(true);
        given(paymentTransactionService.recoverProviderPayment(secondTarget, secondResponse)).willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(2);
        verify(paymentTransactionService).findProviderRecoveryTargets(FIXED_NOW.minusMinutes(5), 2);
        verify(paymentTransactionService).recoverProviderPayment(firstTarget, firstResponse);
        verify(paymentTransactionService).recoverProviderPayment(secondTarget, secondResponse);
    }

    @Test
    void missingProviderPaymentIsRecoveredAsNotFound() {
        PaymentRecoveryTarget target = recoveryTarget(PaymentProviderType.NICEPAY, 100L, "ORD-1");

        given(paymentTransactionService.findProviderRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(target));
        given(paymentProviderRouter.lookup(PaymentProviderType.NICEPAY, new PaymentProviderLookupRequest(null, "ORD-1")))
                .willReturn(Optional.empty());
        given(paymentTransactionService.recoverProviderPaymentNotFound(target)).willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(1);
        verify(paymentTransactionService).recoverProviderPaymentNotFound(target);
        verify(paymentTransactionService, never()).recoverProviderPayment(target, null);
    }

    @Test
    void providerLookupFailureKeepsPaymentUnknown() {
        PaymentRecoveryTarget target = recoveryTarget(PaymentProviderType.NICEPAY, 100L, "ORD-1");
        PaymentProviderException exception = PaymentProviderException.timeout("NICEPAY_TIMEOUT", "NICEPAY timeout");

        given(paymentTransactionService.findProviderRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(target));
        given(paymentProviderRouter.lookup(PaymentProviderType.NICEPAY, new PaymentProviderLookupRequest(null, "ORD-1")))
                .willThrow(exception);
        given(paymentTransactionService.keepProviderPaymentUnknown(target, "NICEPAY_TIMEOUT", "NICEPAY timeout"))
                .willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(1);
        verify(paymentTransactionService).keepProviderPaymentUnknown(target, "NICEPAY_TIMEOUT", "NICEPAY timeout");
    }

    @Test
    void disabledSchedulerDoesNotRecoverPayments() {
        ReflectionTestUtils.setField(paymentRecoveryService, "recoveryEnabled", false);

        paymentRecoveryService.recoverScheduledPayments();

        verify(paymentTransactionService, never()).findProviderRecoveryTargets(FIXED_NOW.minusMinutes(5), 2);
    }

    private PaymentRecoveryTarget recoveryTarget(PaymentProviderType providerType, Long paymentId, String orderNumber) {
        return new PaymentRecoveryTarget(providerType, paymentId, 10L, 1L, orderNumber, 30_000);
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
}
