package com.team08.backend.domain.payment.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.payment.success-outbox")
public record PaymentSuccessOutboxProperties(
        @DefaultValue("true") boolean schedulerEnabled,
        @DefaultValue("1000") long fixedDelay,
        @DefaultValue("20") int batchSize,
        @DefaultValue("5") int maxRetries,
        @DefaultValue("10") long retryBaseDelaySeconds,
        @DefaultValue("600") long retryMaxDelaySeconds
) {
}
