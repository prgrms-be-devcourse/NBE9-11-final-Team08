package com.team08.backend.domain.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt.refresh-cookie")
public record RefreshCookieProperties(
        String name,
        boolean secure,
        String sameSite,
        String domain
) {
}
