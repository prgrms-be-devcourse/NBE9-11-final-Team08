package com.team08.backend.domain.payment.dto.nicepay;

public record NicepayPreparePaymentRequest(
        String payMethod,
        Long issuedCouponId
) {
}
