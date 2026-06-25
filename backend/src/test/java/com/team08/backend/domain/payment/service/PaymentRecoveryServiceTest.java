package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentRecoveryTarget;
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
    private TossPaymentClient tossPaymentClient;

    private PaymentRecoveryService paymentRecoveryService;

    @BeforeEach
    void setUp() {
        paymentRecoveryService = new PaymentRecoveryService(
                paymentTransactionService,
                tossPaymentClient,
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(paymentRecoveryService, "staleThreshold", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(paymentRecoveryService, "batchSize", 2);
        ReflectionTestUtils.setField(paymentRecoveryService, "recoveryEnabled", true);
    }

    @Test
    void recoversTossPaymentByBatchLimit() {
        TossPaymentRecoveryTarget firstTarget = recoveryTarget(100L, "ORD-1");
        TossPaymentRecoveryTarget secondTarget = recoveryTarget(101L, "ORD-2");
        TossPaymentResponse firstResponse = tossResponse("ORD-1", "DONE");
        TossPaymentResponse secondResponse = tossResponse("ORD-2", "DONE");

        given(paymentTransactionService.findTossRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(firstTarget, secondTarget));
        given(tossPaymentClient.findByOrderId("ORD-1")).willReturn(Optional.of(firstResponse));
        given(tossPaymentClient.findByOrderId("ORD-2")).willReturn(Optional.of(secondResponse));
        given(paymentTransactionService.recoverTossPayment(firstTarget, firstResponse)).willReturn(true);
        given(paymentTransactionService.recoverTossPayment(secondTarget, secondResponse)).willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(2);
        verify(paymentTransactionService).findTossRecoveryTargets(FIXED_NOW.minusMinutes(5), 2);
        verify(paymentTransactionService).recoverTossPayment(firstTarget, firstResponse);
        verify(paymentTransactionService).recoverTossPayment(secondTarget, secondResponse);
    }

    @Test
    void missingTossPaymentIsRecoveredAsNotFound() {
        TossPaymentRecoveryTarget target = recoveryTarget(100L, "ORD-1");

        given(paymentTransactionService.findTossRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(target));
        given(tossPaymentClient.findByOrderId("ORD-1")).willReturn(Optional.empty());
        given(paymentTransactionService.recoverTossPaymentNotFound(target)).willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(1);
        verify(paymentTransactionService).recoverTossPaymentNotFound(target);
        verify(paymentTransactionService, never()).recoverTossPayment(target, null);
    }

    @Test
    void tossLookupFailureKeepsPaymentUnknown() {
        TossPaymentRecoveryTarget target = recoveryTarget(100L, "ORD-1");
        TossPaymentException exception = TossPaymentException.timeout("TOSS_TIMEOUT", "Toss timeout");

        given(paymentTransactionService.findTossRecoveryTargets(FIXED_NOW.minusMinutes(5), 2))
                .willReturn(List.of(target));
        given(tossPaymentClient.findByOrderId("ORD-1")).willThrow(exception);
        given(paymentTransactionService.keepTossPaymentUnknown(target, "TOSS_TIMEOUT", "Toss timeout"))
                .willReturn(true);

        int recoveredCount = paymentRecoveryService.recoverPayments();

        assertThat(recoveredCount).isEqualTo(1);
        verify(paymentTransactionService).keepTossPaymentUnknown(target, "TOSS_TIMEOUT", "Toss timeout");
    }

    @Test
    void disabledSchedulerDoesNotRecoverPayments() {
        ReflectionTestUtils.setField(paymentRecoveryService, "recoveryEnabled", false);

        paymentRecoveryService.recoverScheduledPayments();

        verify(paymentTransactionService, never()).findTossRecoveryTargets(FIXED_NOW.minusMinutes(5), 2);
    }

    private TossPaymentRecoveryTarget recoveryTarget(Long paymentId, String orderNumber) {
        return new TossPaymentRecoveryTarget(paymentId, 10L, 1L, orderNumber, 30_000);
    }

    private TossPaymentResponse tossResponse(String orderNumber, String status) {
        return new TossPaymentResponse(
                "payment-key",
                orderNumber,
                status,
                "CARD",
                30_000,
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }
}
