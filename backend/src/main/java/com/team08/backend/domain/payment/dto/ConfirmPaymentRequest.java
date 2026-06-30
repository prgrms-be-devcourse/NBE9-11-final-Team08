package com.team08.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ConfirmPaymentRequest(
        @Schema(description = "PG 결제 키", example = "payment-key")
        String paymentKey,
        @Schema(description = "결제 수단", example = "CARD")
        String method,
        @Schema(description = "결제 승인 요청 금액", example = "30000")
        int amount,
        @Schema(description = "상품별 적용할 발급 쿠폰 ID 맵 (courseId -> issuedCouponId)")
        Map<Long, Long> itemCouponIds,
        @Schema(description = "주문 전체에 적용할 중복 쿠폰 ID")
        Long stackableCouponId,
        @Schema(description = "같은 결제 요청을 재시도할 때 사용하는 멱등성 키", example = "pay-req-20260624-001")
        String idempotencyKey,
        @Schema(description = "NICEPAY 인증 결과 코드", example = "0000")
        String authResultCode,
        @Schema(description = "NICEPAY 인증 결과 메시지", example = "인증 성공")
        String authResultMsg,
        @Schema(description = "NICEPAY 인증 토큰")
        String authToken,
        @Schema(description = "NICEPAY 인증 거래 ID")
        String txTid,
        @Schema(description = "NICEPAY 상점 ID")
        String mid,
        @Schema(description = "NICEPAY 주문번호")
        String moid,
        @Schema(description = "NICEPAY 인증 응답 서명")
        String signature,
        @Schema(description = "NICEPAY 승인 요청 URL")
        String nextAppUrl,
        @Schema(description = "NICEPAY 망취소 요청 URL")
        String netCancelUrl,
        @Schema(description = "NICEPAY 결제수단", example = "CARD")
        String payMethod
) {
    public ConfirmPaymentRequest(String paymentKey, String method, int amount, java.util.Map<Long, Long> itemCouponIds, Long stackableCouponId) {
        this(paymentKey, method, amount, itemCouponIds, stackableCouponId, null);
    }

    public ConfirmPaymentRequest(String paymentKey, String method, int amount, java.util.Map<Long, Long> itemCouponIds, Long stackableCouponId, String idempotencyKey) {
        this(
                paymentKey,
                method,
                amount,
                itemCouponIds,
                stackableCouponId,
                idempotencyKey,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
