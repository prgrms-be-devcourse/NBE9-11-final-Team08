package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentErrorResponse;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.util.NicepaySignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class NicepayPaymentClient {

    private static final String PAYMENT_LOOKUP_PATH = "/v1/payments/{paymentKey}";
    private static final String ORDER_LOOKUP_PATH = "/v1/payments/orders/{orderId}";
    private static final String UNKNOWN_ERROR_CODE = "NICEPAY_UNKNOWN";
    private static final String TIMEOUT_ERROR_CODE = "NICEPAY_TIMEOUT";
    private static final String PARSE_ERROR_CODE = "NICEPAY_RESPONSE_PARSE_FAILED";
    private static final String INVALID_CONFIRM_RESPONSE_CODE = "NICEPAY_INVALID_CONFIRM_RESPONSE";
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
            MultiValueMap<String, String> approvalForm = buildApprovalForm(request, ediDate);
            logNicepayConfirmRequest(request, ediDate, approvalForm);
            String responseBody = restClient.post()
                    .uri(request.nextAppUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(approvalForm)
                    .exchange((httpRequest, response) -> {
                        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        log.info(
                                "NICEPAY confirm HTTP response. status={}, contentType={}, location={}, body={}",
                                response.getStatusCode().value(),
                                response.getHeaders().getContentType(),
                                response.getHeaders().getLocation(),
                                body
                        );
                        if (response.getStatusCode().isError()) {
                            NicepayPaymentErrorResponse errorResponse = parseErrorResponse(body);
                            log.warn("NICEPAY confirm HTTP error. status={}, resultCode={}, resultMsg={}, message={}, body={}",
                                    response.getStatusCode().value(),
                                    errorCode(errorResponse),
                                    errorResponse.resultMsg(),
                                    errorResponse.message(),
                                    body);
                            throw PaymentProviderException.unknown(errorCode(errorResponse), errorMessage(errorResponse));
                        }
                        return body;
                    });
            if (!StringUtils.hasText(responseBody)) {
                throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY confirm response is empty.");
            }
            NicepayPaymentResponse response = parseConfirmResponse(responseBody);
            logNicepayConfirmResponse(response, responseBody);
            return response;
        } catch (PaymentProviderException e) {
            throw e;
        } catch (ResourceAccessException e) {
            // TODO: 승인 요청이 PG에 전달됐을 수 있으므로 NetCancelURL 망취소를 별도 트랜잭션 흐름과 함께 검토한다.
            log.warn("NICEPAY confirm timed out. tid={}, mid={}, moid={}, amount={}, payMethod={}",
                    request.approvalTid(), request.mid(), request.moid(), request.amount(), request.payMethod());
            throw PaymentProviderException.timeout(TIMEOUT_ERROR_CODE, "NICEPAY confirm response timed out.");
        } catch (RestClientResponseException e) {
            NicepayPaymentErrorResponse errorResponse = parseErrorResponse(e);
            log.warn("NICEPAY confirm HTTP error. status={}, resultCode={}, resultMsg={}, message={}, body={}",
                    e.getStatusCode().value(),
                    errorCode(errorResponse),
                    errorResponse.resultMsg(),
                    errorResponse.message(),
                    e.getResponseBodyAsString(StandardCharsets.UTF_8));
            throw PaymentProviderException.unknown(errorCode(errorResponse), errorMessage(errorResponse));
        } catch (RestClientException e) {
            log.warn("NICEPAY confirm result is unclear.", e);
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

    private void logNicepayConfirmRequest(
            NicepayConfirmPaymentRequest request,
            String ediDate,
            MultiValueMap<String, String> form
    ) {
        List<String> parameterNames = form.keySet().stream().sorted().toList();
        log.info(
                "NICEPAY confirm request. url={}, method=POST, contentType={}, parameterKeys={}, authTokenPresent={}, mid={}, moid={}, amount={}, tid={}, signature={}, ediDate={}, nextAppUrl={}, netCancelUrl={}",
                request.nextAppUrl(),
                MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                parameterNames,
                StringUtils.hasText(request.authToken()),
                request.mid(),
                request.moid(),
                request.amount(),
                request.approvalTid(),
                form.getFirst("SignData"),
                ediDate,
                request.nextAppUrl(),
                request.netCancelUrl()
        );
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
        return parseErrorResponse(exception.getResponseBodyAsString(StandardCharsets.UTF_8));
    }

    private NicepayPaymentErrorResponse parseErrorResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, NicepayPaymentErrorResponse.class);
        } catch (JsonProcessingException e) {
            return new NicepayPaymentErrorResponse(UNKNOWN_ERROR_CODE, null, "NICEPAY error response cannot be parsed.");
        }
    }

    NicepayPaymentResponse parseConfirmResponse(String responseBody) {
        if (looksLikeDocumentResponse(responseBody)) {
            log.warn("NICEPAY confirm response is not payment result JSON. body={}", responseBody);
            throw PaymentProviderException.unknown(
                    INVALID_CONFIRM_RESPONSE_CODE,
                    "NICEPAY confirm response is not a payment result."
            );
        }
        try {
            return objectMapper.readValue(responseBody, NicepayPaymentResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("NICEPAY confirm response parse failed. body={}", responseBody, e);
            throw PaymentProviderException.unknown(PARSE_ERROR_CODE, "NICEPAY confirm response cannot be parsed.");
        }
    }

    private boolean looksLikeDocumentResponse(String responseBody) {
        String trimmed = responseBody.stripLeading();
        return trimmed.startsWith("<!DOCTYPE")
                || trimmed.startsWith("<html")
                || trimmed.startsWith("<!--")
                || trimmed.startsWith("/*");
    }

    private void logNicepayConfirmResponse(NicepayPaymentResponse response, String responseBody) {
        log.info(
                "NICEPAY confirm response. resultCode={}, resultMsg={}, tid={}, mid={}, moid={}, amount={}, signature={}, payMethod={}, easyPayCl={}, easyPayMethod={}, selectPayMethod={}, body={}",
                response.resultCode(),
                response.resultMsg(),
                response.resolvedPaymentKey(),
                response.mid(),
                response.resolvedOrderId(),
                response.amount(),
                response.signature(),
                response.payMethod(),
                response.easyPayCl(),
                response.easyPayMethod(),
                response.selectPayMethod(),
                responseBody
        );
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
