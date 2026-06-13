package com.team08.backend.domain.order.controller;

import com.team08.backend.domain.order.dto.CreateDirectOrderRequest;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.dto.OrderSummaryResponse;
import com.team08.backend.domain.order.service.OrderService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "주문", description = "주문 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/cart")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "장바구니 주문 생성", description = "현재 로그인 사용자의 장바구니 항목으로 결제 대기 주문을 생성합니다.")
    public OrderDetailResponse createOrderFromCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return orderService.createOrderFromCart(principal.user().id());
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "바로 주문 생성", description = "강의 ID 하나로 결제 대기 주문을 생성합니다.")
    public OrderDetailResponse createDirectOrder(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody CreateDirectOrderRequest request
    ) {
        return orderService.createDirectOrder(principal.user().id(), request.courseId());
    }

    @GetMapping
    @Operation(summary = "내 주문 목록 조회", description = "현재 로그인 사용자의 주문 목록을 최신 주문순으로 조회합니다.")
    public List<OrderSummaryResponse> getMyOrders(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return orderService.getMyOrders(principal.user().id());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "내 주문 상세 조회", description = "현재 로그인 사용자의 주문 상세와 주문 항목 목록을 조회합니다.")
    public OrderDetailResponse getMyOrder(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return orderService.getMyOrder(principal.user().id(), orderId);
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "결제 전 주문 취소", description = "결제 대기 상태인 현재 로그인 사용자의 주문을 취소합니다.")
    public OrderDetailResponse cancelMyOrder(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "주문 ID", example = "1")
            @PathVariable Long orderId
    ) {
        return orderService.cancelMyOrder(principal.user().id(), orderId);
    }
}
