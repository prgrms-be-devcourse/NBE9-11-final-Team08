package com.team08.backend.domain.auth.service;

import com.team08.backend.domain.auth.entity.RefreshToken;
import com.team08.backend.domain.auth.exception.LoginFailedException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.domain.auth.token.TokenHasher;
import com.team08.backend.domain.auth.token.TokenProperties;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-06-12T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        TokenProperties tokenProperties = new TokenProperties(
                "test-secret-key-test-secret-key-test-secret-key",
                3600000L,
                1209600000L
        );

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtProvider,
                refreshTokenRepository,
                tokenProperties,
                fixedClock
        );
    }

    @Test
    void 로그인에_성공하면_토큰쌍을_반환하고_refreshToken을_저장한다() {
        // given
        String email = "test@email.com";
        String encodedPassword = "encoded-password";
        String password = "password";

        User user = UserFixture.builder()
                .email(email)
                .password(encodedPassword)
                .build();

        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);
        given(jwtProvider.generateTokenPair(user.getId())).willReturn(tokenPair);

        // when
        TokenPair result = authService.login(email, password);

        // then
        assertThat(result).isEqualTo(tokenPair);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        then(refreshTokenRepository).should().save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash())
                .isEqualTo(TokenHasher.hash("refresh-token"));
        assertThat(savedToken.getExpiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 26, 9, 0));
    }

    @Test
    void 존재하지_않는_이메일이면_로그인에_실패한다() {
        // given
        String email = "none@email.com";
        String password = "password";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(LoginFailedException.class);

        then(passwordEncoder).shouldHaveNoInteractions();
        then(jwtProvider).shouldHaveNoInteractions();
        then(refreshTokenRepository).shouldHaveNoInteractions();
    }

    @Test
    void 비밀번호가_일치하지_않으면_로그인에_실패한다() {
        // given
        String email = "test@email.com";
        String password = "wrong-password";

        User user = UserFixture.builder()
                .email(email)
                .password("encoded-password")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(LoginFailedException.class);

        then(jwtProvider).shouldHaveNoInteractions();
        then(refreshTokenRepository).shouldHaveNoInteractions();
    }
}
