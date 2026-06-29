package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.util.NicepaySignature;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NicepayPaymentClientTest {

    @Test
    void approvalFormUsesOfficialNicepayAuthenticationFields() {
        NicepayPaymentClient client = new NicepayPaymentClient(
                mock(RestClient.class),
                new ObjectMapper(),
                new NicepayPaymentProperties("https://api.nicepay.co.kr", "nicepay00m", "merchant-key", null, null),
                Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
        NicepayConfirmPaymentRequest request = new NicepayConfirmPaymentRequest(
                "tid-1",
                "ORD-1",
                30_000,
                "0000",
                "OK",
                "auth-token",
                "tid-1",
                "nicepay00m",
                "ORD-1",
                "auth-signature",
                "https://web.nicepay.co.kr/approve",
                "https://web.nicepay.co.kr/net-cancel",
                "CARD"
        );

        MultiValueMap<String, String> form = client.buildApprovalForm(request, "20260618190000");

        assertThat(form.getFirst("TID")).isEqualTo("tid-1");
        assertThat(form.getFirst("AuthToken")).isEqualTo("auth-token");
        assertThat(form.getFirst("MID")).isEqualTo("nicepay00m");
        assertThat(form.getFirst("Amt")).isEqualTo("30000");
        assertThat(form.getFirst("EdiDate")).isEqualTo("20260618190000");
        assertThat(form.getFirst("CharSet")).isEqualTo("utf-8");
        assertThat(form.getFirst("EdiType")).isEqualTo("JSON");
        assertThat(form.getFirst("SignData")).isEqualTo(
                NicepaySignature.sha256("auth-token" + "nicepay00m" + "30000" + "20260618190000" + "merchant-key")
        );
    }
}
