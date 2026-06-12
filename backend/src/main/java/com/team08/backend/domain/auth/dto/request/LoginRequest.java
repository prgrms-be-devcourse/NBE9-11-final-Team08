package com.team08.backend.domain.auth.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
