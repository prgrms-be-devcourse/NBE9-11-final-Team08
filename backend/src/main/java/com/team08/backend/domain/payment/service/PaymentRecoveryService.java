package com.team08.backend.domain.payment.service;

import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderLookupResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderRouter;
import com.team08.backend.domain.payment.service.PaymentTransactionService.PaymentRecoveryTarget;
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
    private final PaymentProviderRouter paymentProviderRouter;
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
        List<PaymentRecoveryTarget> targets =
                paymentTransactionService.findProviderRecoveryTargets(threshold, batchSize);

        int recoveredCount = 0;
        for (PaymentRecoveryTarget target : targets) {
            if (recoverPayment(target)) {
                recoveredCount++;
            }
        }
        return recoveredCount;
    }

    private boolean recoverPayment(PaymentRecoveryTarget target) {
        try {
            Optional<PaymentProviderLookupResponse> providerPayment = paymentProviderRouter.lookup(
                    target.providerType(),
                    new PaymentProviderLookupRequest(null, target.orderNumber())
            );
            if (providerPayment.isEmpty()) {
                return paymentTransactionService.recoverProviderPaymentNotFound(target);
            }
            return paymentTransactionService.recoverProviderPayment(target, providerPayment.get());
        } catch (PaymentProviderException e) {
            return paymentTransactionService.keepProviderPaymentUnknown(
                    target,
                    e.getFailureCode(),
                    e.getFailureMessage()
            );
        }
    }
}
