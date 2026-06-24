package com.team08.backend.domain.category.controller;

import com.team08.backend.domain.category.dto.CategoryResponse;
import com.team08.backend.domain.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "카테고리", description = "카테고리 조회 API")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리(id·이름·상위·depth)를 반환합니다.")
    @GetMapping("/api/categories")
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories();
    }
}
