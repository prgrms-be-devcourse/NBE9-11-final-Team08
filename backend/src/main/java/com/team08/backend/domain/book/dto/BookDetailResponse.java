package com.team08.backend.domain.book.dto;

import com.team08.backend.domain.book.entity.Book;
import com.team08.backend.domain.category.entity.Category;
import java.util.Optional;

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
                getCategoryName(book.getCategory()),
                book.getViewCount()
        );
    }

    private static String getCategoryName(Category category) {
        return Optional.ofNullable(category)
                .map(Category::getName)
                .orElse("미분류");
    }
}