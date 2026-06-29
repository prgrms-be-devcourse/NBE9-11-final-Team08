package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentErrorResponse;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.util.NicepaySignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class NicepayPaymentClient {

    private static final String PAYMENT_LOOKUP_PATH = "/v1/payments/{paymentKey}";
    private static final String ORDER_LOOKUP_PATH = "/v1/payments/orders/{orderId}";
    private static final String UNKNOWN_ERROR_CODE = "NICEPAY_UNKNOWN";
    private static final String TIMEOUT_ERROR_CODE = "NICEPAY_TIMEOUT";
    private static final String NICEPAY_CHARSET = "utf-8";
    private static final String NICEPAY_EDI_TYPE = "JSON";
    private static final DateTimeFormatter EDI_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final NicepayPaymentProperties properties;
    private final Clock clock;

    public NicepayPaymentClient(
            @Qualifier("nicepayRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            NicepayPaymentProperties properties,
            Clock clock
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public NicepayPaymentResponse confirm(NicepayConfirmPaymentRequest request) {
        try {
            String ediDate = LocalDateTime.now(clock).format(EDI_DATE_FORMATTER);
            NicepayPaymentResponse response = restClient.post()
                    .uri(request.nextAppUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildApprovalForm(request, ediDate))
                    .retrieve()
                    .body(NicepayPaymentResponse.class);
            if (response == null) {
                throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY confirm response is empty.");
            }
            return response;
        } catch (PaymentProviderException e) {
            throw e;
        } catch (ResourceAccessException e) {
            // TODO: 승인 요청이 PG에 도달했을 수 있으므로 NetCancelURL 망취소 정책을 별도 트랜잭션 흐름과 함께 확정한다.
            throw PaymentProviderException.timeout(TIMEOUT_ERROR_CODE, "NICEPAY confirm response timed out.");
        } catch (RestClientResponseException e) {
            NicepayPaymentErrorResponse errorResponse = parseErrorResponse(e);
            throw PaymentProviderException.unknown(errorCode(errorResponse), errorMessage(errorResponse));
        } catch (RestClientException e) {
            throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY confirm result is unclear.");
        }
    }

    MultiValueMap<String, String> buildApprovalForm(NicepayConfirmPaymentRequest request, String ediDate) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        String amount = String.valueOf(request.amount());
        form.add("TID", request.approvalTid());
        form.add("AuthToken", request.authToken());
        form.add("MID", request.mid());
        form.add("Amt", amount);
        form.add("EdiDate", ediDate);
        form.add("SignData", NicepaySignature.sha256(request.authToken() + request.mid() + amount + ediDate + properties.merchantKey()));
        form.add("CharSet", NICEPAY_CHARSET);
        form.add("EdiType", NICEPAY_EDI_TYPE);
        return form;
    }

    public Optional<NicepayPaymentResponse> findByPaymentKey(String paymentKey) {
        return find(PAYMENT_LOOKUP_PATH, paymentKey);
    }

    public Optional<NicepayPaymentResponse> findByOrderId(String orderId) {
        return find(ORDER_LOOKUP_PATH, orderId);
    }

    private Optional<NicepayPaymentResponse> find(String path, String value) {
        try {
            NicepayPaymentResponse response = restClient.get()
                    .uri(path, value)
                    .retrieve()
                    .body(NicepayPaymentResponse.class);
            return Optional.ofNullable(response);
        } catch (ResourceAccessException e) {
            throw PaymentProviderException.timeout(TIMEOUT_ERROR_CODE, "NICEPAY lookup response timed out.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            NicepayPaymentErrorResponse errorResponse = parseErrorResponse(e);
            throw PaymentProviderException.unknown(errorCode(errorResponse), errorMessage(errorResponse));
        } catch (RestClientException e) {
            throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY lookup result is unclear.");
        }
    }

    private NicepayPaymentErrorResponse parseErrorResponse(RestClientResponseException exception) {
        try {
            return objectMapper.readValue(exception.getResponseBodyAsString(), NicepayPaymentErrorResponse.class);
        } catch (JsonProcessingException e) {
            return new NicepayPaymentErrorResponse(UNKNOWN_ERROR_CODE, null, "NICEPAY error response cannot be parsed.");
        }
    }

    private String errorCode(NicepayPaymentErrorResponse response) {
        return StringUtils.hasText(response.resultCode()) ? response.resultCode() : UNKNOWN_ERROR_CODE;
    }

    private String errorMessage(NicepayPaymentErrorResponse response) {
        if (StringUtils.hasText(response.resultMsg())) {
            return response.resultMsg();
        }
        if (StringUtils.hasText(response.message())) {
            return response.message();
        }
        return "NICEPAY confirm result is unclear.";
    }
}
