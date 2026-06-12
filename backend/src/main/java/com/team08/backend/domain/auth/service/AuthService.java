package com.team08.backend.domain.auth.service;

import com.team08.backend.domain.auth.dto.request.SignupRequest;
import com.team08.backend.domain.auth.entity.RefreshToken;
import com.team08.backend.domain.auth.exception.DuplicateEmailException;
import com.team08.backend.domain.auth.exception.InvalidSignupRoleException;
import com.team08.backend.domain.auth.exception.LoginFailedException;
import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.auth.repository.RefreshTokenRepository;
import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.domain.auth.token.TokenHasher;
import com.team08.backend.domain.auth.token.TokenProperties;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider;

    private final RefreshTokenRepository refreshTokenRepository;

    private final TokenProperties tokenProperties;

    private final Clock clock;

    @Transactional
    public TokenPair login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(LoginFailedException::new);

        validatePassword(password, user);

        LoginUserDto loginUser = new LoginUserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
        TokenPair tokenPair = jwtProvider.generateTokenPair(loginUser);

        saveRefreshToken(user, tokenPair.refreshToken());

        return tokenPair;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user;
        switch (request.userRole()) {
            case USER ->
                user = User.createUser(request.email(), encodedPassword, request.nickname(), request.profileImage());
            case SELLER ->
                user = User.createSeller(request.email(), encodedPassword, request.nickname(), request.profileImage());
            default -> throw new InvalidSignupRoleException();
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    private void validatePassword(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new LoginFailedException();
        }
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.create(
                user,
                TokenHasher.hash(refreshToken),
                LocalDateTime.now(clock)
                        .plus(Duration.ofMillis(tokenProperties.refreshExpirationMillis()))
        );

        refreshTokenRepository.save(token);
    }
}
