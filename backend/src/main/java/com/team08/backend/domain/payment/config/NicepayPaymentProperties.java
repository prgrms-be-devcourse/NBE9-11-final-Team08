package com.team08.backend.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payment.nicepay")
public record NicepayPaymentProperties(
        String baseUrl,
        String clientKey,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
