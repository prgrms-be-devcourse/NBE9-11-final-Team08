package com.team08.backend.domain.payment.dto.nicepay;

import java.util.Map;

public record NicepayPreparePaymentRequest(
        String payMethod,
        Map<Long, Long> itemCouponIds,
        Long stackableCouponId
) {
}
