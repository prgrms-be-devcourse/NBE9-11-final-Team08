package com.team08.backend.domain.payment.controller;

import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.FailPaymentRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Tag(name = "결제", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Mock 결제 승인", description = "PG 승인 응답 형태의 요청으로 결제를 성공 처리합니다.")
    public ConfirmPaymentResponse confirmPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @RequestBody ConfirmPaymentRequest request
    ) {
        return paymentService.confirmPayment(principal.user().id(), orderId, request);
    }

    @PostMapping("/{orderId}/fail")
    @Operation(summary = "Mock 결제 실패 처리", description = "결제 실패 결과를 기록하고 주문은 결제 대기 상태로 유지합니다.")
    public PaymentResponse failPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @RequestBody FailPaymentRequest request
    ) {
        return paymentService.failPayment(principal.user().id(), orderId, request);
    }

    @PostMapping("/{orderId}/refund")
    @Operation(summary = "Mock 환불 처리", description = "결제 완료 주문의 기본 환불 상태 전이를 처리합니다.")
    public PaymentResponse refundPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return paymentService.refundPayment(principal.user().id(), orderId);
    }
}
