package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.NicepayPaymentClient;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NicepayPaymentProvider implements PaymentProvider {

    private static final String SUCCESS_RESULT_CODE = "0000";

    private final NicepayPaymentClient nicepayPaymentClient;

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.NICEPAY;
    }

    @Override
    public PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request) {
        NicepayPaymentResponse response = nicepayPaymentClient.confirm(new NicepayConfirmPaymentRequest(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        ));

        if (!SUCCESS_RESULT_CODE.equals(response.resultCode())) {
            throw PaymentProviderException.declined(response.resultCode(), response.resultMsg());
        }

        return new PaymentProviderConfirmResponse(
                response.paymentKey(),
                response.orderId(),
                StringUtils.hasText(response.status()) ? response.status() : "DONE",
                response.method(),
                response.amount(),
                response.approvedAt()
        );
    }
}
