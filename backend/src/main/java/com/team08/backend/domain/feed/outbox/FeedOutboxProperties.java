package com.team08.backend.domain.feed.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.feed.outbox")
public record FeedOutboxProperties(
        int maxRetries,
        long retryBaseDelaySeconds,
        long retryMaxDelaySeconds
) {
    public FeedOutboxProperties {
        if (maxRetries <= 0) {
            maxRetries = 5;
        }
        if (retryBaseDelaySeconds <= 0) {
            retryBaseDelaySeconds = 10;
        }
        if (retryMaxDelaySeconds <= 0) {
            retryMaxDelaySeconds = 600;
        }
    }
}
