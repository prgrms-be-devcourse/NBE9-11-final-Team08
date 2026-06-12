package com.team08.backend.domain.auth.controller;

import com.team08.backend.domain.auth.dto.request.LoginRequest;
import com.team08.backend.domain.auth.dto.request.SignupRequest;
import com.team08.backend.domain.auth.dto.response.LoginResponse;
import com.team08.backend.domain.auth.exception.InvalidRefreshTokenException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.service.AuthService;
import com.team08.backend.domain.auth.token.RefreshTokenCookieFactory;
import com.team08.backend.domain.auth.token.TokenProperties;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.response.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie.name:refreshToken}", required = false)
            String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        TokenPair tokenPair = authService.refresh(refreshToken);
        ResponseCookie cookie = refreshTokenCookieFactory.create(
                tokenPair.refreshToken(),
                Duration.ofMillis(tokenProperties.refreshExpirationMillis())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(LoginResponse.from(tokenPair));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        ResponseCookie deleteCookie = refreshTokenCookieFactory.delete();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ErrorResponse.from(errorCode));
    }
}
