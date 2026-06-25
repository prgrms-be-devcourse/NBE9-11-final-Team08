package com.team08.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? Collections.emptyList() : List.copyOf(allowedOrigins);
    }
}
