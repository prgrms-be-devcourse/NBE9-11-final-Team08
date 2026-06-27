package com.team08.backend.domain.dashboard.controller;

import com.team08.backend.domain.dashboard.dto.SellerAnalyticsResponse;
import com.team08.backend.domain.dashboard.service.SellerDashboardService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[판매자] 대시보드", description = "판매자 본인 강좌의 매출·수강생·인기상품 집계 (판매자 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seller/dashboard")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "판매 분석", description = "선택 기간(range: 3m/6m/1y) 기준 매출·판매 건수·수강생·카테고리 비중·인기 상품을 한 번에 반환.")
    @GetMapping("/analytics")
    public SellerAnalyticsResponse getAnalytics(
            @RequestParam(defaultValue = "6m") String range,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return sellerDashboardService.getAnalytics(principal.user().id(), monthsOf(range));
    }

    private static int monthsOf(String range) {
        return switch (range) {
            case "3m" -> 3;
            case "1y" -> 12;
            default -> 6;
        };
    }
}
