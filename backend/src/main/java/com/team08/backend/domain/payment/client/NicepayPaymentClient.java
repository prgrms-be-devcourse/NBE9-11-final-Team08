package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentErrorResponse;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NicepayPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String UNKNOWN_ERROR_CODE = "NICEPAY_UNKNOWN";
    private static final String TIMEOUT_ERROR_CODE = "NICEPAY_TIMEOUT";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NicepayPaymentClient(
            @Qualifier("nicepayRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public NicepayPaymentResponse confirm(NicepayConfirmPaymentRequest request) {
        try {
            NicepayPaymentResponse response = restClient.post()
                    .uri(CONFIRM_PATH)
                    .body(request)
                    .retrieve()
                    .body(NicepayPaymentResponse.class);
            if (response == null) {
                throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY confirm response is empty.");
            }
            return response;
        } catch (PaymentProviderException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw PaymentProviderException.timeout(TIMEOUT_ERROR_CODE, "NICEPAY confirm response timed out.");
        } catch (RestClientResponseException e) {
            NicepayPaymentErrorResponse errorResponse = parseErrorResponse(e);
            throw PaymentProviderException.unknown(errorCode(errorResponse), errorMessage(errorResponse));
        } catch (RestClientException e) {
            throw PaymentProviderException.unknown(UNKNOWN_ERROR_CODE, "NICEPAY confirm result is unclear.");
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
