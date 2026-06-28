package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.NicepayPaymentClient;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NicepayPaymentProviderTest {

    @Mock
    private NicepayPaymentClient nicepayPaymentClient;

    @Test
    void typeIsNicepay() {
        NicepayPaymentProvider provider = new NicepayPaymentProvider(nicepayPaymentClient);

        assertThat(provider.type()).isEqualTo(PaymentProviderType.NICEPAY);
    }

    @Test
    void successResponseMapsToProviderResponse() {
        NicepayPaymentProvider provider = new NicepayPaymentProvider(nicepayPaymentClient);
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-06-18T19:00:00+09:00");
        given(nicepayPaymentClient.confirm(any())).willReturn(new NicepayPaymentResponse(
                "0000",
                "success",
                "payment-key",
                "order-number",
                null,
                "CARD",
                30_000,
                approvedAt
        ));

        PaymentProviderConfirmResponse response = provider.confirm(
                new PaymentProviderConfirmRequest("payment-key", "order-number", 30_000)
        );

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.totalAmount()).isEqualTo(30_000);
        assertThat(response.approvedAt()).isEqualTo(approvedAt);
    }

    @Test
    void nonSuccessResultMapsToDeclined() {
        NicepayPaymentProvider provider = new NicepayPaymentProvider(nicepayPaymentClient);
        given(nicepayPaymentClient.confirm(any())).willReturn(new NicepayPaymentResponse(
                "3001",
                "card declined",
                "payment-key",
                "order-number",
                "FAILED",
                "CARD",
                30_000,
                null
        ));

        assertThatThrownBy(() -> provider.confirm(new PaymentProviderConfirmRequest("payment-key", "order-number", 30_000)))
                .isInstanceOfSatisfying(PaymentProviderException.class, e -> {
                    assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.DECLINED);
                    assertThat(e.getFailureCode()).isEqualTo("3001");
                });
    }

    @Test
    void timeoutIsPropagatedWithoutRemapping() {
        NicepayPaymentProvider provider = new NicepayPaymentProvider(nicepayPaymentClient);
        PaymentProviderException timeout = PaymentProviderException.timeout("NICEPAY_TIMEOUT", "timeout");
        given(nicepayPaymentClient.confirm(any())).willThrow(timeout);

        assertThatThrownBy(() -> provider.confirm(new PaymentProviderConfirmRequest("payment-key", "order-number", 30_000)))
                .isSameAs(timeout);
    }
}
