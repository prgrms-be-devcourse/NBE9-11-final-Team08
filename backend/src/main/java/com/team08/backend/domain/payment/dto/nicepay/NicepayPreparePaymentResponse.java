package com.team08.backend.domain.payment.dto.nicepay;

public record NicepayPreparePaymentResponse(
        String goodsName,
        int amt,
        String mid,
        String ediDate,
        String moid,
        String signData,
        String payMethod,
        String buyerName,
        String buyerTel,
        String buyerEmail,
        String charSet,
        String reqReserved
) {
}
