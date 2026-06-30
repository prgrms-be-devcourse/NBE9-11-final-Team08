package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.TossPaymentClient;
import com.team08.backend.domain.payment.dto.toss.TossConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentResponse.TossEasyPayResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TossPaymentProviderTest {

    private static final String PAYMENT_KEY = "payment-key";
    private static final String ORDER_NUMBER = "ORD-202606300001";
    private static final OffsetDateTime APPROVED_AT = OffsetDateTime.parse("2026-06-30T12:00:00+09:00");

    private final TossPaymentClient tossPaymentClient = mock(TossPaymentClient.class);
    private final TossPaymentProvider tossPaymentProvider = new TossPaymentProvider(tossPaymentClient);

    @Test
    void confirmUsesEasyPayProviderAsMethodWhenPresent() {
        when(tossPaymentClient.confirm(any(TossConfirmPaymentRequest.class)))
                .thenReturn(tossResponse("간편결제", "카카오페이"));

        PaymentProviderConfirmResponse response = tossPaymentProvider.confirm(
                new PaymentProviderConfirmRequest(PAYMENT_KEY, ORDER_NUMBER, 10_000)
        );

        assertThat(response.method()).isEqualTo("카카오페이");
    }

    @Test
    void lookupUsesEasyPayProviderAsMethodWhenPresent() {
        when(tossPaymentClient.findByPaymentKey(PAYMENT_KEY))
                .thenReturn(Optional.of(tossResponse("간편결제", "카카오페이")));

        Optional<PaymentProviderLookupResponse> response = tossPaymentProvider.lookup(
                new PaymentProviderLookupRequest(PAYMENT_KEY, ORDER_NUMBER)
        );

        assertThat(response).isPresent();
        assertThat(response.get().method()).isEqualTo("카카오페이");
    }

    @Test
    void confirmFallsBackToMethodWhenEasyPayProviderIsBlank() {
        when(tossPaymentClient.confirm(any(TossConfirmPaymentRequest.class)))
                .thenReturn(tossResponse("CARD", " "));

        PaymentProviderConfirmResponse response = tossPaymentProvider.confirm(
                new PaymentProviderConfirmRequest(PAYMENT_KEY, ORDER_NUMBER, 10_000)
        );

        assertThat(response.method()).isEqualTo("CARD");
    }

    private TossPaymentResponse tossResponse(String method, String easyPayProvider) {
        return new TossPaymentResponse(
                PAYMENT_KEY,
                ORDER_NUMBER,
                "DONE",
                method,
                new TossEasyPayResponse(easyPayProvider),
                10_000,
                APPROVED_AT
        );
    }
}
