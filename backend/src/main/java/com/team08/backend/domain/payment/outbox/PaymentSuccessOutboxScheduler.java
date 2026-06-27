package com.team08.backend.domain.payment.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.payment.success-outbox.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentSuccessOutboxScheduler {

    private final PaymentSuccessOutboxProcessor paymentSuccessOutboxProcessor;

    @Scheduled(fixedDelayString = "${app.payment.success-outbox.fixed-delay:1000}")
    public void processPending() {
        paymentSuccessOutboxProcessor.processPending();
    }
}
