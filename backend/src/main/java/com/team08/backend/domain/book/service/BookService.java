package com.team08.backend.domain.book.service;

import com.team08.backend.domain.book.dto.BookCreateRequest;
import com.team08.backend.domain.book.dto.BookUpdateRequest;
import com.team08.backend.domain.book.entity.Book;
import com.team08.backend.domain.book.repository.BookRepository;
import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Long createBook(BookCreateRequest request, User loginUser) {
        if (!"SELLER".equals(loginUser.getRole())) {
            throw new AccessDeniedException("판매자(SELLER) 권한이 필요합니다.");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }

        Book book = Book.builder()
                .seller(loginUser)
                .category(category)
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .description(request.description())
                .price(request.price())
                .isEbook(request.isEbook())
                .build();

        return bookRepository.save(book).getId();
    }

    public Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .filter(book -> book.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 도서입니다."));
    }

    @Transactional
    public void updateBook(Long bookId, BookUpdateRequest request, User loginUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서 상품입니다."));

        if (!book.getSeller().getId().equals(loginUser.getId())) {
            throw new AccessDeniedException("본인이 등록한 도서만 수정할 수 있습니다.");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }

        book.update(
                request.title(),
                request.author(),
                request.publisher(),
                request.description(),
                request.price(),
                request.isEbook(),
                category
        );
    }

    @Transactional
    public void deleteBook(Long bookId, User loginUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다."));

        if (!book.getSeller().getId().equals(loginUser.getId())) {
            throw new AccessDeniedException("본인의 도서만 삭제할 수 있습니다.");
        }

        book.delete();
    }
}