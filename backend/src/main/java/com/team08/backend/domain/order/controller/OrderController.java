package com.team08.backend.domain.order.controller;

import com.team08.backend.domain.order.dto.DirectOrderRequest;
import com.team08.backend.domain.order.dto.OrderDetailResponse;
import com.team08.backend.domain.order.dto.OrderSummaryResponse;
import com.team08.backend.domain.order.service.OrderService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createFromCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return orderService.createFromCart(principal.user().id());
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createDirect(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody DirectOrderRequest request
    ) {
        return orderService.createDirect(principal.user().id(), request.courseId());
    }

    @GetMapping
    public List<OrderSummaryResponse> getMyOrders(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return orderService.getMyOrders(principal.user().id());
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getMyOrder(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long orderId
    ) {
        return orderService.getMyOrder(principal.user().id(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderDetailResponse cancel(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long orderId
    ) {
        return orderService.cancel(principal.user().id(), orderId);
    }
}