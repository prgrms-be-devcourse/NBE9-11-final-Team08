package com.team08.backend.domain.auth.controller;

import com.team08.backend.domain.auth.dto.request.LoginRequest;
import com.team08.backend.domain.auth.dto.response.LoginResponse;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.service.AuthService;
import com.team08.backend.domain.auth.token.RefreshTokenCookieFactory;
import com.team08.backend.domain.auth.token.TokenProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final TokenProperties tokenProperties;

    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
         TokenPair tokenPair = authService.login(request.email(), request.password());

        ResponseCookie cookie = refreshTokenCookieFactory.create(
                tokenPair.refreshToken(),
                Duration.ofMillis(tokenProperties.refreshExpirationMillis())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(LoginResponse.from(tokenPair));
    }
}
