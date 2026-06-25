package com.team08.backend.global.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.auth.token.AccessCookieProperties;
import com.team08.backend.domain.auth.token.JwtProvider;
import com.team08.backend.global.auth.filter.JwtAuthenticationFilter;
import com.team08.backend.global.auth.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccessCookieProperties accessCookieProperties,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> {
            cookie.path("/")
                    .secure(accessCookieProperties.secure())
                    .sameSite(accessCookieProperties.sameSite());
            if (StringUtils.hasText(accessCookieProperties.domain())) {
                cookie.domain(accessCookieProperties.domain());
            }
        });

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/api/auth/csrf",
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/error"
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/me").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/{studyId}").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/by-course/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtProvider jwtProvider,
            AccessCookieProperties accessCookieProperties,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) {
        return new JwtAuthenticationFilter(jwtProvider, accessCookieProperties, jwtAuthenticationEntryPoint);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
