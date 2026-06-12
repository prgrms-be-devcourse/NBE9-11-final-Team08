package com.team08.backend.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.auth.dto.request.LoginRequest;
import com.team08.backend.domain.auth.dto.request.SignupRequest;
import com.team08.backend.domain.auth.dto.request.SignupRole;
import com.team08.backend.domain.auth.dto.response.LoginResponse;
import com.team08.backend.domain.auth.exception.InvalidRefreshTokenException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.service.AuthService;
import com.team08.backend.domain.auth.token.RefreshTokenCookieFactory;
import com.team08.backend.domain.auth.token.TokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    RefreshTokenCookieFactory refreshTokenCookieFactory;

    @MockitoBean
    TokenProperties tokenProperties;

    @Test
    void 로그인에_성공하면_accessToken을_응답하고_refreshToken을_쿠키로_내려준다() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password");
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        given(authService.login(request.email(), request.password()))
                .willReturn(tokenPair);

        given(refreshTokenCookieFactory.create(eq("refresh-token"), any(Duration.class)))
                .willReturn(ResponseCookie.from("refreshToken", "refresh-token")
                        .httpOnly(true)
                        .path("/")
                        .build());

        LoginResponse expected = new LoginResponse("access-token");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(cookie().value("refreshToken", "refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/"));

        then(authService).should()
                .login(request.email(), request.password());
    }

    @Test
    void 이메일이_없으면_400을_반환한다() throws Exception {
        // given
        LoginRequest request = new LoginRequest(
                "",
                "password"
        );

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 회원가입에_성공하면_201을_반환한다() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
                "test@email.com",
                "password",
                "nickname",
                null,
                SignupRole.USER
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        then(authService).should().signup(any(SignupRequest.class));
    }

    @Test
    void refreshToken으로_토큰을_재발급하면_accessToken과_새_refreshToken을_반환한다() throws Exception {
        TokenPair tokenPair = new TokenPair("new-access-token", "new-refresh-token");

        given(authService.refresh("old-refresh-token")).willReturn(tokenPair);
        given(refreshTokenCookieFactory.create(eq("new-refresh-token"), any(Duration.class)))
                .willReturn(ResponseCookie.from("refreshToken", "new-refresh-token")
                        .httpOnly(true)
                        .path("/")
                        .build());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));

        then(authService).should().refresh("old-refresh-token");
    }

    @Test
    void 유효하지_않은_refreshToken이면_401과_삭제_쿠키를_반환한다() throws Exception {
        given(authService.refresh("invalid-refresh-token"))
                .willThrow(new InvalidRefreshTokenException());
        given(refreshTokenCookieFactory.delete())
                .willReturn(ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ZERO)
                        .build());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_005"))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
