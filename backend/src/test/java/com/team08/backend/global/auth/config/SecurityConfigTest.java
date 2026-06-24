package com.team08.backend.global.auth.config;

import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.domain.auth.token.TokenProperties;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.TestController.class)
@EnableConfigurationProperties(TokenProperties.class)
@Import({
        SecurityConfig.class,
        JwtProvider.class,
        SecurityConfigTest.TestController.class
})
class SecurityConfigTest {

    private static final LoginUserDto LOGIN_USER = new LoginUserDto(
            1L,
            "test@example.com",
            "테스트유저",
            "ROLE_USER"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void accessToken이_없으면_인증에_실패한다() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void refresh_요청은_accessToken_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_요청은_accessToken_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 강좌별_스터디_ID_조회는_accessToken_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/api/studies/by-course/{courseId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(100L));
    }

    @Test
    void 유효한_accessToken이면_사용자_정보와_권한으로_인증한다() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(LOGIN_USER);

        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOGIN_USER.id()))
                .andExpect(jsonPath("$.email").value(LOGIN_USER.email()))
                .andExpect(jsonPath("$.name").value(LOGIN_USER.nickname()))
                .andExpect(jsonPath("$.role").value(LOGIN_USER.role()))
                .andExpect(jsonPath("$.authority").value(LOGIN_USER.role()));
    }

    @Test
    void refreshToken으로는_인증할_수_없다() throws Exception {
        String refreshToken = jwtProvider.generateRefreshToken(LOGIN_USER.id());

        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void 위조된_토큰으로는_인증할_수_없다() throws Exception {
        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void bearer_형식이_아니면_인증할_수_없다() throws Exception {
        String accessToken = jwtProvider.generateAccessToken(LOGIN_USER);

        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void 만료된_accessToken으로는_인증할_수_없다() throws Exception {
        JwtProvider expiredTokenProvider = new JwtProvider(new TokenProperties(
                "test-secret-key-test-secret-key-test-secret-key",
                -1000L,
                1209600000L
        ));
        String expiredToken = expiredTokenProvider.generateAccessToken(LOGIN_USER);

        mockMvc.perform(get("/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        TestAuthenticationResponse test(
                @AuthenticationPrincipal LoginUserPrincipal principal
        ) {
            LoginUserDto user = principal.user();
            return new TestAuthenticationResponse(
                    user.id(),
                    user.email(),
                    user.nickname(),
                    user.role(),
                    principal.authorities().iterator().next().getAuthority()
            );
        }

        @PostMapping("/api/auth/refresh")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void refresh() {
        }

        @PostMapping("/api/auth/logout")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void logout() {
        }

        @GetMapping("/api/studies/by-course/{courseId}")
        StudyIdResponse studyId(@PathVariable Long courseId) {
            return new StudyIdResponse(100L);
        }
    }

    record TestAuthenticationResponse(
            Long id,
            String email,
            String name,
            String role,
            String authority
    ) {
    }

    record StudyIdResponse(
            Long studyId
    ) {
    }
}
