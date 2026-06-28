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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize(예: 관리자 전용 엔드포인트)를 실제로 강제한다
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
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/api/payments/toss/webhook")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                // 메트릭 스크랩용. 외부는 nginx 가 403 으로 차단하고(prod.conf.template),
                                // 호스트 포트도 127.0.0.1 바인딩이라 Alloy 가 내부망으로만 접근한다.
                                "/actuator/prometheus",
                                "/api/auth/csrf",
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/payments/toss/webhook",
                                "/error"
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/coupons").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/me").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/{studyId}").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/{studyId}/members").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/studies/by-course/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/videos-local/**").permitAll()
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

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();

        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken
        ) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return plain.resolveCsrfTokenValue(request, csrfToken);
            }
            return xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
