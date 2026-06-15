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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "인증", description = "회원가입, 로그인 및 인증 토큰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final TokenProperties tokenProperties;

    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 액세스 토큰과 리프레시 토큰 쿠키를 발급합니다."
    )
    @SecurityRequirements
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

    @Operation(
            summary = "회원가입",
            description = "사용자 정보를 입력받아 새로운 계정을 생성합니다."
    )
    @SecurityRequirements
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
    }

    @Operation(
            summary = "인증 토큰 갱신",
            description = "리프레시 토큰 쿠키를 검증하고 새로운 액세스 토큰과 리프레시 토큰을 발급합니다."
    )
    @SecurityRequirements
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

    @Operation(
            summary = "로그아웃",
            description = "저장된 리프레시 토큰을 폐기하고 리프레시 토큰 쿠키를 삭제합니다."
    )
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.jwt.refresh-cookie.name:refreshToken}", required = false)
            String refreshToken
    ) {
        authService.logout(refreshToken);
        ResponseCookie deleteCookie = refreshTokenCookieFactory.delete();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
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
