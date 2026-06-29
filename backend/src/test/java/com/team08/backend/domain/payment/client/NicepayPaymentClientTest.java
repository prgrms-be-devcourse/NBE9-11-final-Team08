package com.team08.backend.domain.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.payment.config.NicepayPaymentProperties;
import com.team08.backend.domain.payment.dto.nicepay.NicepayConfirmPaymentRequest;
import com.team08.backend.domain.payment.provider.PaymentProviderException;
import com.team08.backend.domain.payment.util.NicepaySignature;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class NicepayPaymentClientTest {

    @Test
    void approvalFormUsesOfficialNicepayAuthenticationFields() {
        NicepayPaymentClient client = createClient();
        NicepayConfirmPaymentRequest request = createRequest("https://dc1-api.nicepay.co.kr/webapi/pay_process.jsp");

        Map<String, String> form = client.buildApprovalForm(request, "20260618190000");

        assertThat(form.get("TID")).isEqualTo("tid-1");
        assertThat(form.get("AuthToken")).isEqualTo("auth-token");
        assertThat(form.get("MID")).isEqualTo("nicepay00m");
        assertThat(form.get("Amt")).isEqualTo("30000");
        assertThat(form.get("EdiDate")).isEqualTo("20260618190000");
        assertThat(form.get("CharSet")).isEqualTo("utf-8");
        assertThat(form.get("EdiType")).isEqualTo("JSON");
        assertThat(form.get("SignData")).isEqualTo(
                NicepaySignature.sha256("auth-token" + "nicepay00m" + "30000" + "20260618190000" + "merchant-key")
        );
    }

    @Test
    void approvalBodyIsFormUrlEncoded() {
        NicepayPaymentClient client = createClient();
        Map<String, String> form = client.buildApprovalForm(createRequest("https://dc1-api.nicepay.co.kr/webapi/pay_process.jsp"), "20260618190000");

        String body = client.buildApprovalBody(form);

        assertThat(body).contains("TID=tid-1");
        assertThat(body).contains("AuthToken=auth-token");
        assertThat(body).contains("MID=nicepay00m");
        assertThat(body).contains("Amt=30000");
        assertThat(body).contains("EdiDate=20260618190000");
        assertThat(body).contains("CharSet=utf-8");
        assertThat(body).contains("EdiType=JSON");
        assertThat(body).doesNotContain("{");
    }

    @Test
    void confirmEndpointMustBeNicepayApprovalUrl() {
        NicepayPaymentClient client = createClient();

        client.validateConfirmEndpoint("https://dc1-api.nicepay.co.kr/webapi/pay_process.jsp");
        client.validateConfirmEndpoint("https://dc2-api.nicepay.co.kr/webapi/pay_process.jsp");

        assertThatExceptionOfType(PaymentProviderException.class)
                .isThrownBy(() -> client.validateConfirmEndpoint("https://api.nicepay.co.kr/v1/payments/confirm"))
                .satisfies(exception -> assertThat(exception.getFailureCode()).isEqualTo("NICEPAY_CONFIRM_ENDPOINT_INVALID"));
    }

    @Test
    void documentResponseIsRejectedAsInvalidConfirmResponse() {
        NicepayPaymentClient client = createClient();

        assertThatExceptionOfType(PaymentProviderException.class)
                .isThrownBy(() -> client.parseConfirmResponse("/* Licensed to the Apache Software Foundation */"))
                .satisfies(exception -> assertThat(exception.getFailureCode()).isEqualTo("NICEPAY_INVALID_CONFIRM_RESPONSE"));
    }

    @Test
    void jsonSuccessResponseIsParsed() {
        NicepayPaymentClient client = createClient();

        var response = client.parseConfirmResponse("""
                {
                  "ResultCode": "3001",
                  "ResultMsg": "CARD Success",
                  "TID": "tid-1",
                  "MID": "nicepay00m",
                  "Moid": "ORD-1",
                  "Amt": "30000",
                  "Signature": "approval-signature",
                  "PayMethod": "CARD",
                  "UnusedField": "ignored"
                }
                """);

        assertThat(response.resultCode()).isEqualTo("3001");
        assertThat(response.resolvedPaymentKey()).isEqualTo("tid-1");
        assertThat(response.resolvedOrderId()).isEqualTo("ORD-1");
        assertThat(response.amount()).isEqualTo(30_000);
        assertThat(response.payMethod()).isEqualTo("CARD");
    }

    @Test
    void nestedJsonSuccessResponseIsParsed() {
        NicepayPaymentClient client = createClient();

        var response = client.parseConfirmResponse("""
                {
                  "data": {
                    "ResultCode": "3001",
                    "TID": "tid-1",
                    "Moid": "ORD-1",
                    "Amt": "30,000",
                    "PayMethod": "CARD",
                    "ClickpayCl": "16"
                  }
                }
                """);

        assertThat(response.resultCode()).isEqualTo("3001");
        assertThat(response.resolvedPaymentKey()).isEqualTo("tid-1");
        assertThat(response.resolvedOrderId()).isEqualTo("ORD-1");
        assertThat(response.amount()).isEqualTo(30_000);
        assertThat(response.easyPayCl()).isEqualTo("16");
    }

    @Test
    void keyValueSuccessResponseIsParsed() {
        NicepayPaymentClient client = createClient();

        var response = client.parseConfirmResponse(
                "ResultCode=3001&ResultMsg=CARD+Success&TID=tid-1&MID=nicepay00m&Moid=ORD-1&Amt=30000&Signature=approval-signature&PayMethod=CARD"
        );

        assertThat(response.resultCode()).isEqualTo("3001");
        assertThat(response.resultMsg()).isEqualTo("CARD Success");
        assertThat(response.resolvedPaymentKey()).isEqualTo("tid-1");
        assertThat(response.resolvedOrderId()).isEqualTo("ORD-1");
        assertThat(response.amount()).isEqualTo(30_000);
    }

    @Test
    void missingRequiredFieldsAreRejectedWithRawContext() {
        NicepayPaymentClient client = createClient();

        assertThatExceptionOfType(PaymentProviderException.class)
                .isThrownBy(() -> client.parseConfirmResponse("ResultCode=3001&ResultMsg=OK&UnusedField=value"))
                .satisfies(exception -> {
                    assertThat(exception.getFailureCode()).isEqualTo("NICEPAY_RESPONSE_PARSE_FAILED");
                    assertThat(exception.getFailureMessage()).contains("UnusedField");
                    assertThat(exception.getFailureMessage()).contains("bodyPrefix=");
                });
    }

    private NicepayPaymentClient createClient() {
        return new NicepayPaymentClient(
                mock(RestClient.class),
                new ObjectMapper(),
                new NicepayPaymentProperties("https://api.nicepay.co.kr", "nicepay00m", "merchant-key", null, null),
                Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    private NicepayConfirmPaymentRequest createRequest(String nextAppUrl) {
        return new NicepayConfirmPaymentRequest(
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
                nextAppUrl,
                "https://web.nicepay.co.kr/net-cancel",
                "CARD"
        );
    }
}
