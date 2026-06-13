package com.team08.backend.domain.auth.dto.response;

import com.team08.backend.domain.auth.model.TokenPair;

public record LoginResponse(
        String accessToken
) {
    public static LoginResponse from(TokenPair tokenPair) {
        return new LoginResponse(
                tokenPair.accessToken()
        );
    }
}
