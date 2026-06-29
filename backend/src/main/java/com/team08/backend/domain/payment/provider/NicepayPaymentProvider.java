package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.NicepayPaymentClient;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.util.NicepaySignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NicepayPaymentProvider implements PaymentProvider {

    private static final String AUTH_SUCCESS_RESULT_CODE = "0000";
    private static final String CARD_APPROVAL_SUCCESS_RESULT_CODE = "3001";
    private static final String KAKAOPAY_EASY_PAY_CODE = "16";
    private static final String CARD_PAY_METHOD = "CARD";
    private static final Set<String> UNSUPPORTED_PAY_METHODS = Set.of("BANK", "VBANK", "CELLPHONE");
    private static final String DEFAULT_SUCCESS_STATUS = "DONE";
    private static final String DEFAULT_FAILED_STATUS = "FAILED";
    private static final String NICEPAY_AUTH_FAILED = "NICEPAY_AUTH_FAILED";
    private static final String NICEPAY_SIGNATURE_MISMATCH = "NICEPAY_SIGNATURE_MISMATCH";
    private static final String NICEPAY_UNSUPPORTED_PAY_METHOD = "NICEPAY_UNSUPPORTED_PAY_METHOD";
    private static final String NICEPAY_UNRECOGNIZED_RESULT_CODE = "NICEPAY_UNRECOGNIZED_RESULT_CODE";
    private static final String NICEPAY_CONFIG_ERROR = "NICEPAY_CONFIG_ERROR";
    private static final Set<String> CLEAR_DECLINED_RESULT_CODES = Set.of(
            "3011", "3015", "3021", "3022", "3023", "3024", "3025", "3026", "3041", "3043"
    );

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
            log.warn("NICEPAY authentication failed. authResultCode={}, authResultMsg={}, tid={}, mid={}, moid={}, amount={}, payMethod={}",
                    request.authResultCode(), request.authResultMsg(), request.txTid(), request.mid(), request.moid(), request.amount(), request.payMethod());
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

        if (!CARD_APPROVAL_SUCCESS_RESULT_CODE.equals(response.resultCode())) {
            throw approvalFailureException(response);
        }
        validateApprovalSignature(response);

        return new PaymentProviderConfirmResponse(
                response.resolvedPaymentKey(),
                response.resolvedOrderId(),
                StringUtils.hasText(response.status()) ? response.status() : "DONE",
                resolvedMethod(response),
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
                resolvedMethod(response),
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
        if (UNSUPPORTED_PAY_METHODS.contains(payMethod)) {
            throw PaymentProviderException.declined(NICEPAY_UNSUPPORTED_PAY_METHOD, "NICEPAY currently supports CARD and in-window easy pay only.");
        }
    }

    private String resolvedMethod(NicepayPaymentResponse response) {
        if (KAKAOPAY_EASY_PAY_CODE.equals(response.easyPayCl())) {
            return "KAKAOPAY";
        }
        return StringUtils.hasText(response.resolvedMethod()) ? response.resolvedMethod() : CARD_PAY_METHOD;
    }

    private PaymentProviderException approvalFailureException(NicepayPaymentResponse response) {
        if (CLEAR_DECLINED_RESULT_CODES.contains(response.resultCode())) {
            log.warn("NICEPAY approval declined. resultCode={}, resultMsg={}, tid={}, mid={}, moid={}, amount={}, payMethod={}, easyPayCl={}, easyPayMethod={}, selectPayMethod={}",
                    response.resultCode(), response.resultMsg(), response.resolvedPaymentKey(), response.mid(), response.resolvedOrderId(),
                    response.amount(), response.payMethod(), response.easyPayCl(), response.easyPayMethod(), response.selectPayMethod());
            return PaymentProviderException.declined(response.resultCode(), response.resultMsg());
        }
        log.warn("NICEPAY approval result code is unrecognized. resultCode={}, resultMsg={}, tid={}, mid={}, moid={}, amount={}, payMethod={}, easyPayCl={}, easyPayMethod={}, selectPayMethod={}",
                response.resultCode(), response.resultMsg(), response.resolvedPaymentKey(), response.mid(), response.resolvedOrderId(),
                response.amount(), response.payMethod(), response.easyPayCl(), response.easyPayMethod(), response.selectPayMethod());
        return PaymentProviderException.unknown(
                NICEPAY_UNRECOGNIZED_RESULT_CODE,
                "NICEPAY unrecognized ResultCode=" + response.resultCode() + ", ResultMsg=" + response.resultMsg()
        );
    }

    private void validateMerchantKey() {
        if (!StringUtils.hasText(properties.merchantKey())) {
            throw PaymentProviderException.unknown(NICEPAY_CONFIG_ERROR, "NICEPAY MerchantKey is not configured.");
        }
    }

    private void validateAuthenticationSignature(PaymentProviderConfirmRequest request) {
        String expected = NicepaySignature.sha256(request.authToken() + request.mid() + request.amount() + properties.merchantKey());
        if (!expected.equalsIgnoreCase(request.signature())) {
            log.warn("NICEPAY authentication signature mismatch. tid={}, mid={}, moid={}, amount={}, payMethod={}, signature={}",
                    request.txTid(), request.mid(), request.moid(), request.amount(), request.payMethod(), request.signature());
            throw PaymentProviderException.unknown(NICEPAY_SIGNATURE_MISMATCH, "NICEPAY authentication signature does not match.");
        }
    }

    private void validateApprovalSignature(NicepayPaymentResponse response) {
        String mid = StringUtils.hasText(response.mid()) ? response.mid() : properties.mid();
        String expected = NicepaySignature.sha256(
                response.resolvedPaymentKey() + mid + response.amount() + properties.merchantKey()
        );
        if (!expected.equalsIgnoreCase(response.signature())) {
            log.warn("NICEPAY approval signature mismatch. tid={}, mid={}, moid={}, amount={}, payMethod={}, easyPayCl={}, signature={}",
                    response.resolvedPaymentKey(), mid, response.resolvedOrderId(), response.amount(), response.payMethod(), response.easyPayCl(), response.signature());
            throw PaymentProviderException.unknown(NICEPAY_SIGNATURE_MISMATCH, "NICEPAY approval signature does not match.");
        }
    }
}
