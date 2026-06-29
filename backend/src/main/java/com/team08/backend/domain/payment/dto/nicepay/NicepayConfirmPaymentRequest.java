package com.team08.backend.domain.payment.dto.nicepay;

public record NicepayConfirmPaymentRequest(
        String paymentKey,
        String orderId,
        int amount,
        String authResultCode,
        String authResultMsg,
        String authToken,
        String txTid,
        String mid,
        String moid,
        String signature,
        String nextAppUrl,
        String netCancelUrl,
        String payMethod
) {
    public String approvalTid() {
        return txTid != null ? txTid : paymentKey;
    }
}
