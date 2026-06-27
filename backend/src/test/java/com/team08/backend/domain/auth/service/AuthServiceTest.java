package com.team08.backend.domain.auth.service;

import com.team08.backend.domain.auth.dto.request.SignupRequest;
import com.team08.backend.domain.auth.dto.request.SignupRole;
import com.team08.backend.domain.auth.entity.RefreshToken;
import com.team08.backend.domain.auth.exception.LoginFailedException;
import com.team08.backend.domain.auth.exception.InvalidRefreshTokenException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.domain.auth.token.TokenHasher;
import com.team08.backend.domain.auth.token.TokenProperties;
import com.team08.backend.domain.couponreward.outbox.CouponRewardOutboxService;
import com.team08.backend.domain.fixture.UserFixture;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.auth.exception.DuplicateEmailException;
import com.team08.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Mock
    private CouponRewardOutboxService couponRewardOutboxService;

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
                fixedClock,
                couponRewardOutboxService
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
        given(jwtProvider.generateTokenPair(new LoginUserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        ))).willReturn(tokenPair);

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

    @Test
    void 회원가입에_성공한다() {
        // given
        SignupRequest request = new SignupRequest(
                "test@email.com",
                "password",
                "nickname",
                null,
                SignupRole.USER
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(user, "id", 1L);
                    return user;
                });

        // when
        authService.signup(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().saveAndFlush(captor.capture());

        User savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("test@email.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ROLE_USER);
        then(couponRewardOutboxService).should().createUserSignedUpEvent(1L);
    }

    @Test
    void SELLER로_회원가입하면_SELLER로_저장된다() {
        // given
        SignupRequest request = new SignupRequest(
                "test@email.com",
                "password",
                "nickname",
                null,
                SignupRole.SELLER
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(user, "id", 1L);
                    return user;
                });

        // when
        authService.signup(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().saveAndFlush(captor.capture());

        User savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("test@email.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ROLE_SELLER);
    }

    @Test
    void 이미_가입된_이메일이면_회원가입에_실패한다() {
        // given
        SignupRequest request = new SignupRequest(
                "test@email.com",
                "password",
                "nickname",
                null,
                SignupRole.USER
        );

        given(userRepository.existsByEmail(request.email()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        then(passwordEncoder).shouldHaveNoInteractions();
        then(userRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void refreshToken을_재발급하면_기존_토큰을_폐기하고_새_토큰을_저장한다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("old-refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        LoginUserDto loginUser = new LoginUserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
        TokenPair newTokenPair = new TokenPair("new-access-token", "new-refresh-token");

        given(jwtProvider.validateRefreshToken("old-refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("old-refresh-token")))
                .willReturn(Optional.of(storedToken));
        given(jwtProvider.extractUserId("old-refresh-token")).willReturn(user.getId());
        given(jwtProvider.generateTokenPair(loginUser)).willReturn(newTokenPair);

        TokenPair result = authService.refresh("old-refresh-token");

        assertThat(result).isEqualTo(newTokenPair);
        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(storedToken.getRevokedAt()).isEqualTo(LocalDateTime.of(2026, 6, 12, 9, 0));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(captor.capture());
        RefreshToken newStoredToken = captor.getValue();
        assertThat(newStoredToken.getUser()).isEqualTo(user);
        assertThat(newStoredToken.getTokenHash()).isEqualTo(TokenHasher.hash("new-refresh-token"));
        assertThat(newStoredToken.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 26, 9, 0));
    }

    @Test
    void JWT_검증에_실패한_refreshToken은_재발급할_수_없다() {
        given(jwtProvider.validateRefreshToken("invalid-refresh-token")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("invalid-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(refreshTokenRepository).shouldHaveNoInteractions();
    }

    @Test
    void DB에_저장되지_않은_refreshToken은_재발급할_수_없다() {
        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void 폐기된_refreshToken은_재발급할_수_없다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        storedToken.revoke(LocalDateTime.of(2026, 6, 12, 8, 0));

        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void DB_만료시간이_지난_refreshToken은_재발급할_수_없다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 12, 8, 59)
        );

        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void JWT_사용자와_저장된_사용자가_다르면_재발급할_수_없다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );

        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));
        given(jwtProvider.extractUserId("refresh-token")).willReturn(999L);

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(jwtProvider, never()).generateTokenPair(any());
    }

    @Test
    void 한번_사용한_refreshToken은_다시_재발급할_수_없다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        TokenPair newTokenPair = new TokenPair("new-access-token", "new-refresh-token");

        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));
        given(jwtProvider.extractUserId("refresh-token")).willReturn(user.getId());
        given(jwtProvider.generateTokenPair(any(LoginUserDto.class))).willReturn(newTokenPair);

        authService.refresh("refresh-token");

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(jwtProvider, times(1)).generateTokenPair(any(LoginUserDto.class));
    }

    @Test
    void 로그아웃하면_저장된_refreshToken을_폐기한다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(storedToken.getRevokedAt()).isEqualTo(LocalDateTime.of(2026, 6, 12, 9, 0));
    }

    @Test
    void 이미_폐기된_refreshToken으로_로그아웃해도_폐기시각을_변경하지_않는다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        LocalDateTime revokedAt = LocalDateTime.of(2026, 6, 12, 8, 0);
        storedToken.revoke(revokedAt);
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        assertThat(storedToken.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void 저장되지_않은_refreshToken으로_로그아웃해도_성공한다() {
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("unknown-token")))
                .willReturn(Optional.empty());

        authService.logout("unknown-token");

        then(jwtProvider).shouldHaveNoInteractions();
    }

    @Test
    void refreshToken이_없어도_로그아웃은_성공한다() {
        authService.logout(null);

        then(refreshTokenRepository).shouldHaveNoInteractions();
        then(jwtProvider).shouldHaveNoInteractions();
    }

    @Test
    void 로그아웃한_refreshToken으로는_재발급할_수_없다() {
        User user = UserFixture.builder().build();
        RefreshToken storedToken = RefreshToken.create(
                user,
                TokenHasher.hash("refresh-token"),
                LocalDateTime.of(2026, 6, 13, 9, 0)
        );
        given(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash("refresh-token")))
                .willReturn(Optional.of(storedToken));
        given(jwtProvider.validateRefreshToken("refresh-token")).willReturn(true);

        authService.logout("refresh-token");

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        then(jwtProvider).should(never()).generateTokenPair(any(LoginUserDto.class));
    }
}
