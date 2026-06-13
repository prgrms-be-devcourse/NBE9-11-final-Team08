package com.team08.backend.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    private final RefreshCookieProperties properties;

    public ResponseCookie create(String refreshToken, Duration maxAge) {
        return ResponseCookie.from(
                        properties.name(),
                        refreshToken
                )
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
