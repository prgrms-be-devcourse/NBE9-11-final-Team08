package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentErrorResponse;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.util.NicepaySignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String INVALID_CONFIRM_ENDPOINT_CODE = "NICEPAY_CONFIRM_ENDPOINT_INVALID";
    private static final String NICEPAY_CHARSET = "utf-8";
    private static final String NICEPAY_EDI_TYPE = "JSON";
    private static final DateTimeFormatter EDI_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String NICEPAY_CONFIRM_PATH = "/webapi/pay_process.jsp";
    private static final List<String> NICEPAY_CONFIRM_HOSTS = List.of(
            "dc1-api.nicepay.co.kr",
            "dc2-api.nicepay.co.kr"
    );

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
            validateConfirmEndpoint(request.nextAppUrl());
            String ediDate = LocalDateTime.now(clock).format(EDI_DATE_FORMATTER);
            Map<String, String> approvalForm = buildApprovalForm(request, ediDate);
            String approvalBody = buildApprovalBody(approvalForm);
            logNicepayConfirmRequest(request, ediDate, approvalForm);
            String responseBody = restClient.post()
                    .uri(request.nextAppUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                    .body(approvalBody)
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

    Map<String, String> buildApprovalForm(NicepayConfirmPaymentRequest request, String ediDate) {
        Map<String, String> form = new LinkedHashMap<>();
        String amount = String.valueOf(request.amount());
        form.put("TID", request.approvalTid());
        form.put("AuthToken", request.authToken());
        form.put("MID", request.mid());
        form.put("Amt", amount);
        form.put("EdiDate", ediDate);
        form.put("SignData", NicepaySignature.sha256(request.authToken() + request.mid() + amount + ediDate + properties.merchantKey()));
        form.put("CharSet", NICEPAY_CHARSET);
        form.put("EdiType", NICEPAY_EDI_TYPE);
        return form;
    }

    String buildApprovalBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private void logNicepayConfirmRequest(
            NicepayConfirmPaymentRequest request,
            String ediDate,
            Map<String, String> form
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
                form.get("SignData"),
                ediDate,
                request.nextAppUrl(),
                request.netCancelUrl()
        );
    }

    void validateConfirmEndpoint(String nextAppUrl) {
        if (!StringUtils.hasText(nextAppUrl)) {
            throw PaymentProviderException.unknown(INVALID_CONFIRM_ENDPOINT_CODE, "NICEPAY confirm endpoint is empty.");
        }
        URI uri;
        try {
            uri = URI.create(nextAppUrl);
        } catch (IllegalArgumentException e) {
            throw PaymentProviderException.unknown(INVALID_CONFIRM_ENDPOINT_CODE, "NICEPAY confirm endpoint is invalid.");
        }
        boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                && NICEPAY_CONFIRM_HOSTS.contains(uri.getHost())
                && NICEPAY_CONFIRM_PATH.equals(uri.getPath())
                && uri.getQuery() == null
                && uri.getFragment() == null;
        if (!valid) {
            throw PaymentProviderException.unknown(INVALID_CONFIRM_ENDPOINT_CODE, "NICEPAY confirm endpoint is not allowed.");
        }
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
            if (looksLikeJsonResponse(responseBody)) {
                return toNicepayPaymentResponse(flattenJsonResponse(responseBody), responseBody);
            }
            if (looksLikeKeyValueResponse(responseBody)) {
                return toNicepayPaymentResponse(parseKeyValueResponse(responseBody), responseBody);
            }
            throw PaymentProviderException.unknown(
                    INVALID_CONFIRM_RESPONSE_CODE,
                    "NICEPAY confirm response is not a payment result."
            );
        } catch (JsonProcessingException e) {
            log.warn("NICEPAY confirm response parse failed. body={}", responseBody, e);
            throw parseFailed(responseBody, Map.of());
        } catch (IllegalArgumentException e) {
            log.warn("NICEPAY confirm response mapping failed. body={}", responseBody, e);
            throw parseFailed(responseBody, Map.of());
        }
    }

    private NicepayPaymentResponse toNicepayPaymentResponse(Map<String, String> rawValues, String responseBody) {
        Map<String, String> normalized = normalizeConfirmResponse(rawValues);
        validateRequiredConfirmFields(normalized, rawValues, responseBody);
        return NicepayPaymentResponse.fromNormalized(normalized);
    }

    private Map<String, String> flattenJsonResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        Map<String, String> values = new LinkedHashMap<>();
        flattenJsonNode("", root, values);
        return values;
    }

    private void flattenJsonNode(String prefix, JsonNode node, Map<String, String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenJsonNode(key, entry.getValue(), values);
            });
            return;
        }
        if (node.isValueNode()) {
            values.put(prefix, node.asText());
        }
    }

    private Map<String, String> normalizeConfirmResponse(Map<String, String> values) {
        Map<String, String> normalized = new LinkedHashMap<>();
        normalized.put("resultCode", firstValue(values, "ResultCode", "resultCode"));
        normalized.put("resultMsg", firstValue(values, "ResultMsg", "resultMsg"));
        normalized.put("paymentKey", firstValue(values, "paymentKey", "TID", "Tid", "tid", "TxTid", "txTid"));
        normalized.put("tid", firstValue(values, "TID", "Tid", "tid", "TxTid", "txTid"));
        normalized.put("mid", firstValue(values, "MID", "Mid", "mid"));
        normalized.put("orderId", firstValue(values, "orderId", "Moid", "MOID", "moid"));
        normalized.put("moid", firstValue(values, "Moid", "MOID", "moid", "orderId"));
        normalized.put("amount", firstValue(values, "Amt", "amt", "amount"));
        normalized.put("signature", firstValue(values, "Signature", "signature"));
        normalized.put("payMethod", firstValue(values, "PayMethod", "payMethod"));
        normalized.put("easyPayCl", firstValue(values, "EasyPayCl", "ClickpayCl", "easyPayCl", "clickpayCl"));
        normalized.put("easyPayMethod", firstValue(values, "EasyPayMethod", "easyPayMethod"));
        normalized.put("selectPayMethod", firstValue(values, "SelectPayMethod", "selectPayMethod"));
        return normalized;
    }

    private String firstValue(Map<String, String> values, String... names) {
        for (String name : names) {
            String value = values.get(name);
            if (StringUtils.hasText(value)) {
                return value;
            }
            Optional<String> nestedValue = values.entrySet().stream()
                    .filter(entry -> entry.getKey().endsWith("." + name))
                    .map(Map.Entry::getValue)
                    .filter(StringUtils::hasText)
                    .findFirst();
            if (nestedValue.isPresent()) {
                return nestedValue.get();
            }
        }
        return null;
    }

    private void validateRequiredConfirmFields(
            Map<String, String> normalized,
            Map<String, String> rawValues,
            String responseBody
    ) {
        if (!StringUtils.hasText(normalized.get("resultCode"))
                || !StringUtils.hasText(normalized.get("tid"))
                || !StringUtils.hasText(normalized.get("moid"))
                || !StringUtils.hasText(normalized.get("amount"))) {
            throw parseFailed(responseBody, rawValues);
        }
    }

    private PaymentProviderException parseFailed(String responseBody, Map<String, String> values) {
        String message = "NICEPAY confirm response cannot be parsed. keys="
                + values.keySet()
                + ", bodyPrefix="
                + bodyPrefix(responseBody);
        return PaymentProviderException.unknown(PARSE_ERROR_CODE, message);
    }

    private String bodyPrefix(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        return responseBody.length() <= 500 ? responseBody : responseBody.substring(0, 500);
    }

    private boolean looksLikeJsonResponse(String responseBody) {
        String trimmed = responseBody.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private boolean looksLikeKeyValueResponse(String responseBody) {
        String trimmed = responseBody.strip();
        return trimmed.contains("=") && !trimmed.contains("\n<");
    }

    private Map<String, String> parseKeyValueResponse(String responseBody) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : responseBody.strip().split("[&\\r\\n]+")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            values.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
        }
        if (values.isEmpty()) {
            throw PaymentProviderException.unknown(
                    INVALID_CONFIRM_RESPONSE_CODE,
                    "NICEPAY confirm response is not a payment result."
            );
        }
        return values;
    }

    private boolean looksLikeDocumentResponse(String responseBody) {
        String trimmed = responseBody.stripLeading();
        return trimmed.startsWith("<!DOCTYPE")
                || trimmed.startsWith("<html")
                || trimmed.startsWith("<!--")
                || trimmed.startsWith("/*")
                || trimmed.startsWith("function ")
                || trimmed.startsWith("(function");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
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
