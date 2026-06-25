package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.service.PaymentTransactionService.TossPaymentRecoveryTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private final PaymentTransactionService paymentTransactionService;
    private final TossPaymentClient tossPaymentClient;
    private final Clock clock;

    @Value("${app.payment.recovery.enabled:true}")
    private boolean recoveryEnabled;

    @Value("${app.payment.recovery.stale-threshold:PT5M}")
    private Duration staleThreshold;

    @Value("${app.payment.recovery.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.payment.recovery.fixed-delay:60000}")
    public void recoverScheduledPayments() {
        if (!recoveryEnabled) {
            return;
        }

        recoverPayments();
    }

    public int recoverPayments() {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(staleThreshold);
        List<TossPaymentRecoveryTarget> targets =
                paymentTransactionService.findTossRecoveryTargets(threshold, batchSize);

        int recoveredCount = 0;
        for (TossPaymentRecoveryTarget target : targets) {
            if (recoverPayment(target)) {
                recoveredCount++;
            }
        }
        return recoveredCount;
    }

    private boolean recoverPayment(TossPaymentRecoveryTarget target) {
        try {
            Optional<TossPaymentResponse> tossPayment = tossPaymentClient.findByOrderId(target.orderNumber());
            if (tossPayment.isEmpty()) {
                return paymentTransactionService.recoverTossPaymentNotFound(target);
            }
            return paymentTransactionService.recoverTossPayment(target, tossPayment.get());
        } catch (TossPaymentException e) {
            return paymentTransactionService.keepTossPaymentUnknown(
                    target,
                    e.getFailureCode(),
                    e.getFailureMessage()
            );
        }
    }
}
