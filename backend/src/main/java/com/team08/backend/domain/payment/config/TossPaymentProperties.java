package com.team08.backend.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payment.toss")
public record TossPaymentProperties(
        String baseUrl,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
