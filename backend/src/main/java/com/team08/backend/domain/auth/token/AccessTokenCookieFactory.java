package com.team08.backend.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AccessTokenCookieFactory {

    private final AccessCookieProperties properties;

    public String name() {
        return properties.name();
    }

    public ResponseCookie create(String accessToken, Duration maxAge) {
        return ResponseCookie.from(properties.name(), accessToken)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(properties.name(), "")
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
