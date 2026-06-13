package com.team08.backend.domain.auth.token;

import com.team08.backend.domain.auth.model.TokenPair;
import com.team08.backend.domain.user.dto.LoginUserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "nickname";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtProvider(TokenProperties properties) {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT Secret Key must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMillis = properties.expirationMillis();
        this.refreshTokenExpirationMillis = properties.refreshExpirationMillis();
    }

    public TokenPair generateTokenPair(LoginUserDto loginUser) {
        return new TokenPair(
                generateAccessToken(loginUser),
                generateRefreshToken(loginUser.id())
        );
    }

    public String generateAccessToken(LoginUserDto loginUser) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(loginUser.id()))
                .claim(EMAIL_CLAIM, loginUser.email())
                .claim(NAME_CLAIM, loginUser.nickname())
                .claim(ROLE_CLAIM, loginUser.role())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public LoginUserDto extractLoginUser(String token) {
        Claims claims = parseClaims(token);
        return new LoginUserDto(
                Long.valueOf(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                claims.get(NAME_CLAIM, String.class),
                claims.get(ROLE_CLAIM, String.class)
        );
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TOKEN_TYPE);
    }

    private boolean validateTokenType(String token, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token);
            return expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
