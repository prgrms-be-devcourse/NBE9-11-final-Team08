package com.team08.backend.domain.auth.model;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
