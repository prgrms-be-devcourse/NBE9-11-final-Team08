package com.team08.backend.domain.auth.service;

import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenPair login(String email, String password) {
        return new TokenPair("", "");
    }
}
