package com.team08.backend.domain.payment.dto.nicepay;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

public record NicepayPaymentResponse(
        @JsonAlias("ResultCode")
        String resultCode,
        @JsonAlias("ResultMsg")
        String resultMsg,
        String paymentKey,
        @JsonAlias("TID")
        String tid,
        String orderId,
        @JsonAlias("Moid")
        String moid,
        String status,
        String method,
        @JsonAlias("PayMethod")
        String payMethod,
        @JsonAlias("MID")
        String mid,
        @JsonAlias("Signature")
        String signature,
        @JsonAlias("Amt")
        long amount,
        OffsetDateTime approvedAt
) {
    public String resolvedPaymentKey() {
        return paymentKey != null ? paymentKey : tid;
    }

    public String resolvedOrderId() {
        return orderId != null ? orderId : moid;
    }

    public String resolvedMethod() {
        if (method != null) {
            return method;
        }
        return payMethod;
    }
}
