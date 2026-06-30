package com.team08.backend.domain.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class NicepayPaymentConfig {

    private static final String DEFAULT_BASE_URL = "https://api.nicepay.co.kr";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient nicepayRestClient(NicepayPaymentProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(defaultIfNull(properties.connectTimeout(), DEFAULT_CONNECT_TIMEOUT));
        requestFactory.setReadTimeout(defaultIfNull(properties.readTimeout(), DEFAULT_READ_TIMEOUT));

        return RestClient.builder()
                .baseUrl(StringUtils.hasText(properties.baseUrl()) ? properties.baseUrl() : DEFAULT_BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.merchantKey())) {
                        headers.setBasicAuth(properties.merchantKey(), "");
                    }
                })
                .build();
    }

    private Duration defaultIfNull(Duration value, Duration defaultValue) {
        return value == null ? defaultValue : value;
    }
}
