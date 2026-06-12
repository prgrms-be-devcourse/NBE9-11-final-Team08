package com.team08.backend.domain.auth.token;

import com.team08.backend.domain.auth.model.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        TokenProperties properties = new TokenProperties(
                "test-secret-key-test-secret-key-test-secret-key",
                3600000L,
                1209600000L
        );

        jwtProvider = new JwtProvider(properties);
    }

    @Test
    void accessToken을_생성한다() {
        // when
        String token = jwtProvider.generateAccessToken(1L);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void accessToken에서_userId를_추출한다() {
        // given
        Long userId = 1L;
        String token = jwtProvider.generateAccessToken(userId);

        // when
        Long extractedUserId = jwtProvider.extractUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
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
    void 유효한_토큰이면_true를_반환한다() {
        // given
        String token = jwtProvider.generateAccessToken(1L);

        // when
        boolean result = jwtProvider.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 잘못된_토큰이면_false를_반환한다() {
        // given
        String invalidToken = "invalid.token.value";

        // when
        boolean result = jwtProvider.validateToken(invalidToken);

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

        String token = anotherProvider.generateAccessToken(1L);

        // when
        boolean result = jwtProvider.validateToken(token);

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

        String expiredToken = expiredTokenProvider.generateAccessToken(1L);

        // when
        boolean result = jwtProvider.validateToken(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 토큰쌍을_생성한다() {
        // when
        TokenPair tokenPair = jwtProvider.generateTokenPair(1L);

        // then
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
    }
}
