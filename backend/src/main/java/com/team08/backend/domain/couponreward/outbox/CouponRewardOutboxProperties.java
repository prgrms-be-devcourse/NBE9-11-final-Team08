package com.team08.backend.domain.couponreward.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.coupon-reward.outbox")
public record CouponRewardOutboxProperties(
        int batchSize,
        int maxRetries,
        long retryBaseDelaySeconds,
        long retryMaxDelaySeconds
) {
    public CouponRewardOutboxProperties {
        if (batchSize <= 0) {
            batchSize = 100;
        }
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
