package com.team08.backend.domain.auth.token;

import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.user.dto.LoginUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtProviderTest {

    private JwtProvider jwtProvider;
    private LoginUserDto loginUser;

    @BeforeEach
    void setUp() {
        TokenProperties properties = new TokenProperties(
                "test-secret-key-test-secret-key-test-secret-key",
                3600000L,
                1209600000L
        );

        jwtProvider = new JwtProvider(properties);
        loginUser = new LoginUserDto(
                1L,
                "test@example.com",
                "테스트유저",
                "ROLE_USER"
        );
    }

    @Test
    void accessToken을_생성한다() {
        // when
        String token = jwtProvider.generateAccessToken(loginUser);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void accessToken에서_로그인_사용자_정보를_추출한다() {
        // given
        String token = jwtProvider.generateAccessToken(loginUser);

        // when
        LoginUserDto extractedUser = jwtProvider.extractLoginUser(token);

        // then
        assertThat(extractedUser).isEqualTo(loginUser);
    }

    @Test
    void refreshToken에서_userId를_추출한다() {
        // given
        Long userId = 1L;
        String token = jwtProvider.generateRefreshToken(userId);

        // when
        Long extractedUserId = jwtProvider.extractUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void accessToken은_accessToken으로_검증된다() {
        // given
        String token = jwtProvider.generateAccessToken(loginUser);

        // when
        boolean result = jwtProvider.validateAccessToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void refreshToken은_accessToken으로_검증되지_않는다() {
        // given
        String token = jwtProvider.generateRefreshToken(loginUser.id());

        // when
        boolean result = jwtProvider.validateAccessToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void accessToken은_refreshToken으로_검증되지_않는다() {
        // given
        String token = jwtProvider.generateAccessToken(loginUser);

        // when
        boolean result = jwtProvider.validateRefreshToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 잘못된_토큰은_accessToken으로_검증되지_않는다() {
        // given
        String invalidToken = "invalid.token.value";

        // when
        boolean result = jwtProvider.validateAccessToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 다른_secret으로_생성한_토큰이면_false를_반환한다() {
        // given
        JwtProvider anotherProvider = new JwtProvider(
                new TokenProperties(
                        "another-secret-key-another-secret-key-another-secret-key",
                        3600000L,
                        1209600000L
                )
        );

        String token = anotherProvider.generateAccessToken(loginUser);

        // when
        boolean result = jwtProvider.validateAccessToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 만료된_토큰이면_false를_반환한다() {
        // given
        JwtProvider expiredTokenProvider = new JwtProvider(
                new TokenProperties(
                        "test-secret-key-test-secret-key-test-secret-key",
                        -1000L,
                        1209600000L
                )
        );

        String expiredToken = expiredTokenProvider.generateAccessToken(loginUser);

        // when
        boolean result = jwtProvider.validateAccessToken(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 토큰쌍을_생성한다() {
        // when
        TokenPair tokenPair = jwtProvider.generateTokenPair(loginUser);

        // then
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(jwtProvider.validateAccessToken(tokenPair.accessToken())).isTrue();
        assertThat(jwtProvider.validateRefreshToken(tokenPair.refreshToken())).isTrue();
    }
}
