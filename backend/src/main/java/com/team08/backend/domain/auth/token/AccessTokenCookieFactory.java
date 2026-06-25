package com.team08.backend.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AccessTokenCookieFactory {

    private final AccessCookieProperties properties;

    public String name() {
        return properties.name();
    }

    public ResponseCookie create(String accessToken, Duration maxAge) {
        return baseCookie(accessToken)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie delete() {
        return baseCookie("")
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.name(), value);
        if (StringUtils.hasText(properties.domain())) {
            builder.domain(properties.domain());
        }
        return builder;
    }
}
