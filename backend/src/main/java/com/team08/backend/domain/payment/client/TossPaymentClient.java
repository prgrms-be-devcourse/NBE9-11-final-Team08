package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.TossPaymentProperties;
import com.team08.backend.domain.payment.dto.toss.TossConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentErrorResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class TossPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String UNKNOWN_ERROR_CODE = "TOSS_UNKNOWN";
    private static final String TIMEOUT_ERROR_CODE = "TOSS_TIMEOUT";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TossPaymentClient(TossPaymentProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(defaultIfNull(properties.connectTimeout(), Duration.ofSeconds(3)));
        requestFactory.setReadTimeout(defaultIfNull(properties.readTimeout(), Duration.ofSeconds(5)));

        this.restClient = RestClient.builder()
                .baseUrl(StringUtils.hasText(properties.baseUrl()) ? properties.baseUrl() : "https://api.tosspayments.com")
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.secretKey())) {
                        headers.setBasicAuth(properties.secretKey(), "");
                    }
                })
                .build();
        this.objectMapper = objectMapper;
    }

    private Duration defaultIfNull(Duration value, Duration defaultValue) {
        return value == null ? defaultValue : value;
    }

    public TossPaymentResponse confirm(TossConfirmPaymentRequest request) {
        try {
            TossPaymentResponse response = restClient.post()
                    .uri(CONFIRM_PATH)
                    .body(request)
                    .retrieve()
                    .body(TossPaymentResponse.class);
            if (response == null) {
                throw TossPaymentException.unknown(UNKNOWN_ERROR_CODE, "Toss Payments 승인 응답이 비어 있습니다.");
            }
            return response;
        } catch (ResourceAccessException e) {
            throw TossPaymentException.timeout(TIMEOUT_ERROR_CODE, "Toss Payments 승인 응답 시간이 초과되었습니다.");
        } catch (RestClientResponseException e) {
            TossPaymentErrorResponse errorResponse = parseErrorResponse(e);
            if (e.getStatusCode().is4xxClientError()) {
                throw TossPaymentException.declined(errorResponse.code(), errorResponse.message());
            }
            throw TossPaymentException.unknown(errorResponse.code(), errorResponse.message());
        } catch (RestClientException e) {
            throw TossPaymentException.unknown(UNKNOWN_ERROR_CODE, "Toss Payments 승인 결과를 확인할 수 없습니다.");
        }
    }

    private TossPaymentErrorResponse parseErrorResponse(RestClientResponseException exception) {
        try {
            return objectMapper.readValue(exception.getResponseBodyAsString(), TossPaymentErrorResponse.class);
        } catch (JsonProcessingException e) {
            return new TossPaymentErrorResponse(UNKNOWN_ERROR_CODE, "Toss Payments 오류 응답을 해석할 수 없습니다.");
        }
    }
}
