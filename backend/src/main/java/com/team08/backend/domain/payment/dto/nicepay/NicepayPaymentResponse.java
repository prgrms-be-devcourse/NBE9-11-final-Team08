package com.team08.backend.domain.payment.dto.nicepay;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;

public record NicepayPaymentResponse(
        @JsonAlias({"ResultCode", "resultCode"})
        String resultCode,
        @JsonAlias({"ResultMsg", "resultMsg"})
        String resultMsg,
        String paymentKey,
        @JsonAlias({"TID", "tid"})
        String tid,
        String orderId,
        @JsonAlias({"Moid", "moid"})
        String moid,
        String status,
        String method,
        @JsonAlias({"PayMethod", "payMethod"})
        String payMethod,
        @JsonAlias({"EasyPayCl", "easyPayCl", "ClickpayCl", "clickpayCl"})
        String easyPayCl,
        @JsonAlias({"EasyPayMethod", "easyPayMethod"})
        String easyPayMethod,
        @JsonAlias({"SelectPayMethod", "selectPayMethod"})
        String selectPayMethod,
        @JsonAlias({"MID", "mid"})
        String mid,
        @JsonAlias({"Signature", "signature"})
        String signature,
        @JsonAlias({"Amt", "amount"})
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
        if (easyPayMethod != null) {
            return easyPayMethod;
        }
        if (selectPayMethod != null) {
            return selectPayMethod;
        }
        return payMethod;
    }
}
