package com.team08.backend.domain.payment.controller;

import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Tag(name = "결제", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Mock 결제 승인", description = "현재 로그인 사용자의 결제 대기 주문을 Mock 결제로 승인합니다.")
    public ConfirmPaymentResponse confirmPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return paymentService.confirmPayment(principal.user().id(), orderId);
    }
}
