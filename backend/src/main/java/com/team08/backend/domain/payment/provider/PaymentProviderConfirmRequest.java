package com.team08.backend.domain.payment.provider;

public record PaymentProviderConfirmRequest(
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
    public PaymentProviderConfirmRequest(String paymentKey, String orderId, int amount) {
        this(paymentKey, orderId, amount, null, null, null, null, null, null, null, null, null, null);
    }
}
