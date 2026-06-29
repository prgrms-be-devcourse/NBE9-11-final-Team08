package com.team08.backend.domain.payment.provider;

import com.team08.backend.domain.payment.client.NicepayPaymentClient;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentResponse;
import com.team08.backend.domain.payment.util.NicepaySignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NicepayPaymentProviderTest {

    private static final String MERCHANT_KEY = "merchant-key";
    private static final String MID = "nicepay00m";
    private static final String AUTH_TOKEN = "auth-token";
    private static final String TID = "tid-1";

    private NicepayPaymentClient nicepayPaymentClient;
    private NicepayPaymentProvider nicepayPaymentProvider;

    @BeforeEach
    void setUp() {
        nicepayPaymentClient = mock(NicepayPaymentClient.class);
        nicepayPaymentProvider = new NicepayPaymentProvider(
                nicepayPaymentClient,
                new NicepayPaymentProperties("https://api.nicepay.co.kr", MID, MERCHANT_KEY, null, null)
        );
    }

    @Test
    void cardApprovalSuccessCodeMapsToCommonProviderResponse() {
        PaymentProviderConfirmRequest request = nicepayConfirmRequest(authSignature());
        NicepayConfirmPaymentRequest nicepayRequest = toNicepayRequest(request);
        given(nicepayPaymentClient.confirm(nicepayRequest))
                .willReturn(nicepayResponse("3001", "OK", "DONE", 30_000, approvalSignature(30_000)));

        PaymentProviderConfirmResponse response = nicepayPaymentProvider.confirm(request);

        assertThat(response.paymentKey()).isEqualTo(TID);
        assertThat(response.orderId()).isEqualTo("ORD-1");
        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.method()).isEqualTo("CARD");
        assertThat(response.totalAmount()).isEqualTo(30_000);
    }

    @Test
    void authFailureDoesNotCallApprovalApiAndThrowsDeclinedException() {
        PaymentProviderConfirmRequest request = new PaymentProviderConfirmRequest(
                TID,
                "ORD-1",
                30_000,
                "9999",
                "auth failed",
                AUTH_TOKEN,
                TID,
                MID,
                "ORD-1",
                authSignature(),
                "https://web.nicepay.co.kr/approve",
                "https://web.nicepay.co.kr/net-cancel",
                "CARD"
        );

        assertThatThrownBy(() -> nicepayPaymentProvider.confirm(request))
                .isInstanceOfSatisfying(PaymentProviderException.class,
                        e -> {
                            assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.DECLINED);
                            assertThat(e.getFailureCode()).isEqualTo("9999");
                        });
        verify(nicepayPaymentClient, never()).confirm(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unsupportedPayMethodIsRejectedBeforeApprovalApi() {
        PaymentProviderConfirmRequest request = new PaymentProviderConfirmRequest(
                TID,
                "ORD-1",
                30_000,
                "0000",
                "OK",
                AUTH_TOKEN,
                TID,
                MID,
                "ORD-1",
                authSignature(),
                "https://web.nicepay.co.kr/approve",
                "https://web.nicepay.co.kr/net-cancel",
                "BANK"
        );

        assertThatThrownBy(() -> nicepayPaymentProvider.confirm(request))
                .isInstanceOfSatisfying(PaymentProviderException.class,
                        e -> assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.DECLINED));
        verify(nicepayPaymentClient, never()).confirm(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authSignatureMismatchThrowsUnknownException() {
        PaymentProviderConfirmRequest request = nicepayConfirmRequest("wrong-signature");

        assertThatThrownBy(() -> nicepayPaymentProvider.confirm(request))
                .isInstanceOfSatisfying(PaymentProviderException.class,
                        e -> {
                            assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.UNKNOWN);
                            assertThat(e.getFailureCode()).isEqualTo("NICEPAY_SIGNATURE_MISMATCH");
                        });
        verify(nicepayPaymentClient, never()).confirm(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approvalSignatureMismatchThrowsUnknownException() {
        PaymentProviderConfirmRequest request = nicepayConfirmRequest(authSignature());
        given(nicepayPaymentClient.confirm(toNicepayRequest(request)))
                .willReturn(nicepayResponse("3001", "OK", "DONE", 30_000, "wrong-signature"));

        assertThatThrownBy(() -> nicepayPaymentProvider.confirm(request))
                .isInstanceOfSatisfying(PaymentProviderException.class,
                        e -> {
                            assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.UNKNOWN);
                            assertThat(e.getFailureCode()).isEqualTo("NICEPAY_SIGNATURE_MISMATCH");
                        });
    }

    @Test
    void approvalFailureCodeThrowsDeclinedException() {
        PaymentProviderConfirmRequest request = nicepayConfirmRequest(authSignature());
        given(nicepayPaymentClient.confirm(toNicepayRequest(request)))
                .willReturn(nicepayResponse("3015", "card declined", "FAILED", 30_000, approvalSignature(30_000)));

        assertThatThrownBy(() -> nicepayPaymentProvider.confirm(request))
                .isInstanceOfSatisfying(PaymentProviderException.class,
                        e -> {
                            assertThat(e.getFailureType()).isEqualTo(PaymentProviderFailureType.DECLINED);
                            assertThat(e.getFailureCode()).isEqualTo("3015");
                        });
    }

    @Test
    void lookupCardSuccessCodeWithoutStatusMapsToDone() {
        given(nicepayPaymentClient.findByPaymentKey(TID))
                .willReturn(Optional.of(nicepayResponse("3001", "OK", null, 30_000, approvalSignature(30_000))));

        Optional<PaymentProviderLookupResponse> response = nicepayPaymentProvider.lookup(
                new PaymentProviderLookupRequest(TID, null)
        );

        assertThat(response).isPresent();
        assertThat(response.get().status()).isEqualTo("DONE");
    }

    private PaymentProviderConfirmRequest nicepayConfirmRequest(String signature) {
        return new PaymentProviderConfirmRequest(
                TID,
                "ORD-1",
                30_000,
                "0000",
                "OK",
                AUTH_TOKEN,
                TID,
                MID,
                "ORD-1",
                signature,
                "https://web.nicepay.co.kr/approve",
                "https://web.nicepay.co.kr/net-cancel",
                "CARD"
        );
    }

    private NicepayConfirmPaymentRequest toNicepayRequest(PaymentProviderConfirmRequest request) {
        return new NicepayConfirmPaymentRequest(
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
        );
    }

    private NicepayPaymentResponse nicepayResponse(
            String resultCode,
            String resultMsg,
            String status,
            long amount,
            String signature
    ) {
        return new NicepayPaymentResponse(
                resultCode,
                resultMsg,
                null,
                TID,
                null,
                "ORD-1",
                status,
                null,
                "CARD",
                MID,
                signature,
                amount,
                OffsetDateTime.parse("2026-06-18T19:00:00+09:00")
        );
    }

    private String authSignature() {
        return NicepaySignature.sha256(AUTH_TOKEN + MID + 30_000 + MERCHANT_KEY);
    }

    private String approvalSignature(long amount) {
        return NicepaySignature.sha256(TID + MID + amount + MERCHANT_KEY);
    }
}
