package com.team08.backend.global.auth.filter;

import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// TODO: 임시 필터로 제거 필요
public class MockLoginUserFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!hasAccessToken(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        LoginUserDto user = new LoginUserDto(
                1L,
                "test@example.com",
                "테스트유저",
                "ROLE_USER"
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        new LoginUserPrincipal(user),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean hasAccessToken(String authorization) {
        return authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && !authorization.substring(BEARER_PREFIX.length()).isBlank();
    }
}
