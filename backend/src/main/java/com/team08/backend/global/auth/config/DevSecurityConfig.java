package com.team08.backend.global.auth.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest; // 👈 추가된 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        return http.build();
    }
}
