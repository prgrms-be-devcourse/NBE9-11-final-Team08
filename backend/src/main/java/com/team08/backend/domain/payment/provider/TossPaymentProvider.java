package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TossPaymentProvider implements PaymentProvider {

    private final TossPaymentClient tossPaymentClient;

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.TOSS;
    }

    @Override
    public PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request) {
        try {
            TossPaymentResponse response = tossPaymentClient.confirm(new TossConfirmPaymentRequest(
                    request.paymentKey(),
                    request.orderId(),
                    request.amount()
            ));
            return new PaymentProviderConfirmResponse(
                    response.paymentKey(),
                    response.orderId(),
                    response.status(),
                    response.method(),
                    response.totalAmount(),
                    response.approvedAt()
            );
        } catch (TossPaymentException e) {
            throw switch (e.getFailureType()) {
                case DECLINED -> PaymentProviderException.declined(e.getFailureCode(), e.getFailureMessage());
                case TIMEOUT -> PaymentProviderException.timeout(e.getFailureCode(), e.getFailureMessage());
                case UNKNOWN -> PaymentProviderException.unknown(e.getFailureCode(), e.getFailureMessage());
            };
        }
    }
}
