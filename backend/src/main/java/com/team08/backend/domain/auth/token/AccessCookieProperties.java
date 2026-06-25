package com.team08.backend.domain.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt.access-cookie")
public record AccessCookieProperties(
        String name,
        boolean secure,
        String sameSite
) {
}
