package com.team08.backend.domain.cart.controller;

import com.team08.backend.domain.cart.dto.AddCartItemRequest;
import com.team08.backend.domain.cart.dto.CartResponse;
import com.team08.backend.domain.cart.service.CartService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Tag(name = "장바구니", description = "장바구니 API")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "장바구니 상품 추가", description = "현재 로그인 사용자의 장바구니에 판매 중인 강의를 추가합니다.")
    public CartResponse addItem(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(principal.user().id(), request.courseId());
    }

    @GetMapping
    @Operation(summary = "내 장바구니 조회", description = "현재 로그인 사용자의 장바구니 상품 목록과 총 금액을 조회합니다.")
    public CartResponse getCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return cartService.getCart(principal.user().id());
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "장바구니 상품 삭제", description = "현재 로그인 사용자의 장바구니에서 특정 상품을 삭제합니다.")
    public CartResponse removeItem(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Parameter(description = "장바구니 항목 ID", example = "1")
            @PathVariable Long cartItemId
    ) {
        return cartService.removeItem(principal.user().id(), cartItemId);
    }

    @DeleteMapping
    @Operation(summary = "장바구니 전체 비우기", description = "현재 로그인 사용자의 장바구니 항목을 모두 삭제합니다.")
    public CartResponse clearCart(
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return cartService.clearCart(principal.user().id());
    }
}
