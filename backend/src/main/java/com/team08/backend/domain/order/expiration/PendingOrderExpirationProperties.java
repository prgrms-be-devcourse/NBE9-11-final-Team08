package com.team08.backend.domain.order.expiration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.order.expiration")
public record PendingOrderExpirationProperties(
        @DefaultValue("true") boolean schedulerEnabled,
        @DefaultValue("60000") long fixedDelay,
        @DefaultValue("30") long expirationMinutes,
        @DefaultValue("20") int batchSize
) {
}
