package com.team08.backend.domain.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookCreateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,
        @NotBlank(message = "도서 제목은 필수입니다.")
        String title,
        @NotBlank(message = "저자는 필수입니다.")
        String author,
        @NotBlank(message = "출판사는 필수입니다.")
        String publisher,
        String description,
        @NotNull(message = "가격은 필수입니다.")
        @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
        Integer price,
        @NotNull(message = "E북 여부는 필수입니다.")
        Boolean isEbook
) {
}