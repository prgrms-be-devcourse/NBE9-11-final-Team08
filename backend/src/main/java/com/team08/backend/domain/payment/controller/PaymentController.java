package com.team08.backend.domain.payment.controller;

import com.team08.backend.domain.payment.dto.MockPaymentFailRequest;
import com.team08.backend.domain.payment.dto.MockPaymentSuccessRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/payments/mock")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/success")
    public PaymentResponse mockSuccess(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) MockPaymentSuccessRequest request
    ) {
        String paymentKey = request == null ? null : request.paymentKey();
        String method = request == null ? null : request.method();
        return paymentService.mockSuccess(principal.user().id(), orderId, paymentKey, method);
    }

    @PostMapping("/fail")
    public PaymentResponse mockFail(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) MockPaymentFailRequest request
    ) {
        String failedReason = request == null ? null : request.failedReason();
        return paymentService.mockFail(principal.user().id(), orderId, failedReason);
    }
}
