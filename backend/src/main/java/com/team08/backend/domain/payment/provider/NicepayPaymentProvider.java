package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.NicepayPaymentClient;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.util.NicepaySignature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NicepayPaymentProvider implements PaymentProvider {

    private static final String AUTH_SUCCESS_RESULT_CODE = "0000";
    private static final String CARD_APPROVAL_SUCCESS_RESULT_CODE = "3001";
    private static final String CARD_PAY_METHOD = "CARD";
    private static final String DEFAULT_SUCCESS_STATUS = "DONE";
    private static final String DEFAULT_FAILED_STATUS = "FAILED";
    private static final String NICEPAY_AUTH_FAILED = "NICEPAY_AUTH_FAILED";
    private static final String NICEPAY_SIGNATURE_MISMATCH = "NICEPAY_SIGNATURE_MISMATCH";
    private static final String NICEPAY_UNSUPPORTED_PAY_METHOD = "NICEPAY_UNSUPPORTED_PAY_METHOD";
    private static final String NICEPAY_CONFIG_ERROR = "NICEPAY_CONFIG_ERROR";

    private final NicepayPaymentClient nicepayPaymentClient;
    private final NicepayPaymentProperties properties;

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.NICEPAY;
    }

    @Override
    public PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request) {
        validatePayMethod(request.payMethod());
        validateMerchantKey();
        if (!AUTH_SUCCESS_RESULT_CODE.equals(request.authResultCode())) {
            throw PaymentProviderException.declined(
                    StringUtils.hasText(request.authResultCode()) ? request.authResultCode() : NICEPAY_AUTH_FAILED,
                    StringUtils.hasText(request.authResultMsg()) ? request.authResultMsg() : "NICEPAY authentication failed."
            );
        }
        validateAuthenticationSignature(request);

        NicepayPaymentResponse response = nicepayPaymentClient.confirm(new NicepayConfirmPaymentRequest(
                request.paymentKey(),
                request.orderId(),
                request.amount(),
                request.authResultCode(),
                request.authResultMsg(),
                request.authToken(),
                request.txTid(),
                request.mid(),
                request.moid(),
                request.signature(),
                request.nextAppUrl(),
                request.netCancelUrl(),
                request.payMethod()
        ));

        validateApprovalSignature(response);

        if (!CARD_APPROVAL_SUCCESS_RESULT_CODE.equals(response.resultCode())) {
            throw PaymentProviderException.declined(response.resultCode(), response.resultMsg());
        }

        return new PaymentProviderConfirmResponse(
                response.resolvedPaymentKey(),
                response.resolvedOrderId(),
                StringUtils.hasText(response.status()) ? response.status() : "DONE",
                StringUtils.hasText(response.resolvedMethod()) ? response.resolvedMethod() : CARD_PAY_METHOD,
                response.amount(),
                response.approvedAt()
        );
    }

    @Override
    public Optional<PaymentProviderLookupResponse> lookup(PaymentProviderLookupRequest request) {
        Optional<NicepayPaymentResponse> response = StringUtils.hasText(request.paymentKey())
                ? nicepayPaymentClient.findByPaymentKey(request.paymentKey())
                : nicepayPaymentClient.findByOrderId(request.orderId());
        return response.map(this::toLookupResponse);
    }

    private PaymentProviderLookupResponse toLookupResponse(NicepayPaymentResponse response) {
        return new PaymentProviderLookupResponse(
                response.resolvedPaymentKey(),
                response.resolvedOrderId(),
                lookupStatus(response),
                response.resolvedMethod(),
                response.amount(),
                response.approvedAt()
        );
    }

    private String lookupStatus(NicepayPaymentResponse response) {
        if (StringUtils.hasText(response.status())) {
            return response.status();
        }
        return CARD_APPROVAL_SUCCESS_RESULT_CODE.equals(response.resultCode()) ? DEFAULT_SUCCESS_STATUS : DEFAULT_FAILED_STATUS;
    }

    private void validatePayMethod(String payMethod) {
        if (!CARD_PAY_METHOD.equals(payMethod)) {
            throw PaymentProviderException.declined(NICEPAY_UNSUPPORTED_PAY_METHOD, "NICEPAY currently supports CARD only.");
        }
    }

    private void validateMerchantKey() {
        if (!StringUtils.hasText(properties.merchantKey())) {
            throw PaymentProviderException.unknown(NICEPAY_CONFIG_ERROR, "NICEPAY MerchantKey is not configured.");
        }
    }

    private void validateAuthenticationSignature(PaymentProviderConfirmRequest request) {
        String expected = NicepaySignature.sha256(request.authToken() + request.mid() + request.amount() + properties.merchantKey());
        if (!expected.equalsIgnoreCase(request.signature())) {
            throw PaymentProviderException.unknown(NICEPAY_SIGNATURE_MISMATCH, "NICEPAY authentication signature does not match.");
        }
    }

    private void validateApprovalSignature(NicepayPaymentResponse response) {
        String expected = NicepaySignature.sha256(
                response.resolvedPaymentKey() + response.mid() + response.amount() + properties.merchantKey()
        );
        if (!expected.equalsIgnoreCase(response.signature())) {
            throw PaymentProviderException.unknown(NICEPAY_SIGNATURE_MISMATCH, "NICEPAY approval signature does not match.");
        }
    }
}
