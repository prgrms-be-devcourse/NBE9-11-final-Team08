package com.team08.backend.domain.book.dto;

public record BookCreateRequest(
        Long categoryId,
        String title,
        String author,
        String publisher,
        String description,
        Integer price,
        Boolean isEbook
) {
}