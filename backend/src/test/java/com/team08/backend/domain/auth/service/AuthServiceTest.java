package com.team08.backend.domain.auth.service;

import com.team08.backend.domain.auth.entity.RefreshToken;
import com.team08.backend.domain.auth.exception.LoginFailedException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthService authService;

    @Test
    void 로그인에_성공하면_토큰쌍을_반환하고_refreshToken을_저장한다() {
        // given
        String email = "test@email.com";
        String password = "password";
        User user = UserFixture.builder()
                .email(email)
                .password(password)
                .build();

        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, user.getPassword())).willReturn(true);
        given(jwtProvider.generateTokenPair(user.getId())).willReturn(tokenPair);

        // when
        TokenPair result = authService.login(email, password);

        // then
        assertThat(result).isEqualTo(tokenPair);

        then(refreshTokenRepository).should().save(any(RefreshToken.class));
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
