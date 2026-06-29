package com.team08.backend.domain.payment.dto.nicepay;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.OffsetDateTime;
import java.util.Map;

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
        OffsetDateTime approvedAt,
        String rawTid,
        String rawMid,
        String rawAmount,
        String rawSignature
) {
    public static NicepayPaymentResponse fromNormalized(Map<String, String> values) {
        return new NicepayPaymentResponse(
                values.get("resultCode"),
                values.get("resultMsg"),
                values.get("paymentKey"),
                values.get("tid"),
                values.get("orderId"),
                values.get("moid"),
                values.get("status"),
                values.get("method"),
                values.get("payMethod"),
                values.get("easyPayCl"),
                values.get("easyPayMethod"),
                values.get("selectPayMethod"),
                values.get("mid"),
                values.get("signature"),
                parseAmount(values.get("amount")),
                null,
                values.get("tid"),
                values.get("mid"),
                values.get("amount"),
                values.get("signature")
        );
    }

    public NicepayPaymentResponse(
            String resultCode,
            String resultMsg,
            String paymentKey,
            String tid,
            String orderId,
            String moid,
            String status,
            String method,
            String payMethod,
            String easyPayCl,
            String easyPayMethod,
            String selectPayMethod,
            String mid,
            String signature,
            long amount,
            OffsetDateTime approvedAt
    ) {
        this(
                resultCode,
                resultMsg,
                paymentKey,
                tid,
                orderId,
                moid,
                status,
                method,
                payMethod,
                easyPayCl,
                easyPayMethod,
                selectPayMethod,
                mid,
                signature,
                amount,
                approvedAt,
                tid,
                mid,
                String.valueOf(amount),
                signature
        );
    }

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

    private static long parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0L;
        }
        return Long.parseLong(amount.trim().replace(",", ""));
    }
}
