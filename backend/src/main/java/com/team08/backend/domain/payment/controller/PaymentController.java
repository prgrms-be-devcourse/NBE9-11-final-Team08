package com.team08.backend.domain.payment.controller;

import com.team08.backend.domain.payment.config.TossPaymentProperties;
import com.team08.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.team08.backend.domain.payment.dto.ConfirmPaymentResponse;
import com.team08.backend.domain.payment.dto.FailPaymentRequest;
import com.team08.backend.domain.payment.dto.PaymentResponse;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPaymentWebhookRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPreparePaymentRequest;
import com.team08.backend.domain.payment.dto.nicepay.NicepayPreparePaymentResponse;
import com.team08.backend.domain.payment.dto.toss.TossPaymentWebhookRequest;
import com.team08.backend.domain.payment.entity.PaymentProviderType;
import com.team08.backend.domain.payment.service.PaymentService;
import com.team08.backend.domain.payment.service.NicepayPaymentWebhookService;
import com.team08.backend.domain.payment.service.NicepayPaymentWebhookService.NicepayPaymentWebhookResult;
import com.team08.backend.domain.payment.service.TossPaymentWebhookService;
import com.team08.backend.domain.payment.service.TossPaymentWebhookService.TossPaymentWebhookResult;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Tag(name = "결제", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentWebhookService tossPaymentWebhookService;
    private final NicepayPaymentWebhookService nicepayPaymentWebhookService;
    private final TossPaymentProperties tossPaymentProperties;

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Mock 결제 승인", description = "Mock 결제 승인 요청으로 결제를 성공 처리합니다.")
    public ConfirmPaymentResponse confirmPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @RequestBody ConfirmPaymentRequest request
    ) {
        return paymentService.confirmPayment(principal.user().id(), orderId, request);
    }

    @PostMapping("/{orderId}/toss/confirm")
    @Operation(summary = "Toss Payments 결제 승인", description = "Toss 결제창 완료 후 Toss 승인 API 결과가 성공일 때만 결제를 완료 처리합니다.")
    public ConfirmPaymentResponse confirmTossPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @RequestBody ConfirmPaymentRequest request
    ) {
        return paymentService.confirmTossPayment(principal.user().id(), orderId, request);
    }

    @PostMapping("/{orderId}/providers/{providerType}/confirm")
    @Operation(
            summary = "Provider 결제 승인",
            description = "주문에 대해 선택된 Provider 승인 API 결과가 성공한 경우에만 결제를 완료 처리합니다."
    )
    public ConfirmPaymentResponse confirmProviderPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @PathVariable PaymentProviderType providerType,
            @RequestBody ConfirmPaymentRequest request
    ) {
        return paymentService.confirmProviderPayment(principal.user().id(), orderId, providerType, request);
    }

    @PostMapping("/{orderId}/nicepay/prepare")
    @Operation(
            summary = "NICEPAY 결제창 파라미터 생성",
            description = "주문 금액과 MerchantKey 기반 서명을 서버에서 생성해 NICEPAY PC 인증결제 form 파라미터를 반환합니다."
    )
    public NicepayPreparePaymentResponse prepareNicepayPayment(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId,
            @RequestBody NicepayPreparePaymentRequest request
    ) {
        return paymentService.prepareNicepayPayment(principal.user(), orderId, request);
    }

    @PostMapping("/toss/webhook")
    @Operation(summary = "Toss Payments webhook", description = "Toss Payments webhook 이벤트를 받아 결제 결과를 다시 조회하고 상태를 보정합니다.")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "X-Toss-Webhook-Secret", required = false) String headerSecret,
            @RequestParam(value = "token", required = false) String token,
            @RequestBody TossPaymentWebhookRequest request
    ) {
        if (!isValidWebhookSecret(headerSecret, token)) {
            return ResponseEntity.status(401).build();
        }

        TossPaymentWebhookResult result = tossPaymentWebhookService.handle(request);
        if (result == TossPaymentWebhookResult.RETRYABLE_FAILURE) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/nicepay/webhook")
    @Operation(summary = "NICEPAY webhook", description = "NICEPAY webhook event is verified by provider lookup before payment state recovery.")
    public ResponseEntity<String> handleNicepayWebhook(
            @RequestBody NicepayPaymentWebhookRequest request
    ) {
        NicepayPaymentWebhookResult result = nicepayPaymentWebhookService.handle(request);
        if (result == NicepayPaymentWebhookResult.RETRYABLE_FAILURE) {
            return ResponseEntity.status(503).body("RETRY");
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/{orderId}/fail")
    @Operation(summary = "Mock 결제 실패 처리", description = "Mock 결제 실패 결과를 기록하고 주문은 결제 대기 상태로 유지합니다.")
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

    private boolean isValidWebhookSecret(String headerSecret, String token) {
        String configuredSecret = tossPaymentProperties.webhookSecret();
        if (!StringUtils.hasText(configuredSecret)) {
            log.error("Toss webhook secret is not configured. Webhook request is rejected.");
            return false;
        }

        return constantTimeEquals(configuredSecret, headerSecret)
                || constantTimeEquals(configuredSecret, token);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(actual)) {
            return false;
        }
        if (expected.length() != actual.length()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
