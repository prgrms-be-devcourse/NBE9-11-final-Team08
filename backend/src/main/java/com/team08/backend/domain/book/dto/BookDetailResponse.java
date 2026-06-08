package com.team08.backend.domain.book.dto;

import com.team08.backend.domain.book.entity.Book;

public record BookDetailResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String description,
        Integer price,
        Boolean isEbook,
        String categoryName,
        Integer viewCount
) {
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getDescription(),
                book.getPrice(),
                book.getIsEbook(),
                book.getCategory() != null ? book.getCategory().getName() : "미분류",
                book.getViewCount()
        );
    }
}