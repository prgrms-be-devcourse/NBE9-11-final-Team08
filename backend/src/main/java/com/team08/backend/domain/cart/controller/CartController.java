package com.team08.backend.domain.cart.controller;

import com.team08.backend.domain.cart.dto.AddCartItemRequest;
import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.service.CartService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import com.team08.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ApiResponse.success(cartService.addItem(principal.user().id(), request.courseId()));
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return ApiResponse.success(cartService.getCart(principal.user().id()));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long cartItemId
    ) {
        return ApiResponse.success(cartService.removeItem(principal.user().id(), cartItemId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return ApiResponse.success(cartService.clearCart(principal.user().id()));
    }
}
