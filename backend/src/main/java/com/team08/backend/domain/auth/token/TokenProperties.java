package com.team08.backend.domain.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record TokenProperties(
        String secret,
        long expirationMillis,
        long refreshExpirationMillis
) {
}