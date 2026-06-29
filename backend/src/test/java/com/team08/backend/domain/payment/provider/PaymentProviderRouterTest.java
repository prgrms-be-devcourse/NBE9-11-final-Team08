package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProviderRouterTest {

    @Test
    void confirmCallsOnlySelectedProvider() {
        AtomicBoolean tossCalled = new AtomicBoolean(false);
        AtomicBoolean nicepayCalled = new AtomicBoolean(false);
        PaymentProviderRouter router = new PaymentProviderRouter(List.of(
                provider(PaymentProviderType.TOSS, tossCalled),
                provider(PaymentProviderType.NICEPAY, nicepayCalled)
        ));

        PaymentProviderConfirmResponse response = router.confirm(
                PaymentProviderType.NICEPAY,
                new PaymentProviderConfirmRequest("payment-key", "order-number", 30_000)
        );

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(tossCalled).isFalse();
        assertThat(nicepayCalled).isTrue();
    }

    @Test
    void unsupportedProviderFailsWithoutFallback() {
        PaymentProviderRouter router = new PaymentProviderRouter(List.of(provider(PaymentProviderType.TOSS, new AtomicBoolean())));

        assertThatThrownBy(() -> router.confirm(
                PaymentProviderType.KCP,
                new PaymentProviderConfirmRequest("payment-key", "order-number", 30_000)
        ))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    private PaymentProvider provider(PaymentProviderType providerType, AtomicBoolean called) {
        return new PaymentProvider() {
            @Override
            public PaymentProviderType type() {
                return providerType;
            }

            @Override
            public PaymentProviderConfirmResponse confirm(PaymentProviderConfirmRequest request) {
                called.set(true);
                return new PaymentProviderConfirmResponse(
                        request.paymentKey(),
                        request.orderId(),
                        "DONE",
                        "CARD",
                        request.amount(),
                        null
                );
            }

            @Override
            public Optional<PaymentProviderLookupResponse> lookup(PaymentProviderLookupRequest request) {
                return Optional.empty();
            }
        };
    }
}
