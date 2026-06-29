package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.client.TossPaymentException;
import com.team08.backend.domain.payment.dto.toss.TossConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

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

    @Override
    public Optional<PaymentProviderLookupResponse> lookup(PaymentProviderLookupRequest request) {
        try {
            Optional<TossPaymentResponse> response = StringUtils.hasText(request.paymentKey())
                    ? tossPaymentClient.findByPaymentKey(request.paymentKey())
                    : tossPaymentClient.findByOrderId(request.orderId());
            return response.map(this::toLookupResponse);
        } catch (TossPaymentException e) {
            throw switch (e.getFailureType()) {
                case DECLINED -> PaymentProviderException.declined(e.getFailureCode(), e.getFailureMessage());
                case TIMEOUT -> PaymentProviderException.timeout(e.getFailureCode(), e.getFailureMessage());
                case UNKNOWN -> PaymentProviderException.unknown(e.getFailureCode(), e.getFailureMessage());
            };
        }
    }

    private PaymentProviderLookupResponse toLookupResponse(TossPaymentResponse response) {
        return new PaymentProviderLookupResponse(
                response.paymentKey(),
                response.orderId(),
                response.status(),
                response.method(),
                response.totalAmount(),
                response.approvedAt()
        );
    }
}
