package com.team08.backend.domain.book.service;

import com.team08.backend.domain.book.dto.BookCreateRequest;
import com.team08.backend.domain.book.dto.BookDetailResponse;
import com.team08.backend.domain.book.dto.BookUpdateRequest;
import com.team08.backend.domain.book.entity.Book;
import com.team08.backend.domain.book.repository.BookRepository;
import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public Long createBook(BookCreateRequest request, Long userId) {
        User loginUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

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

    public BookDetailResponse getBookDetail(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서이거나 삭제된 상태입니다."));
        return BookDetailResponse.from(book);
    }

    @Transactional
    public void updateBook(Long bookId, BookUpdateRequest request, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서 상품입니다."));

        if (!book.getSeller().getId().equals(userId)) {
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
    public void deleteBook(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서입니다."));

        if (!book.getSeller().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 도서만 삭제할 수 있습니다.");
        }

        book.delete();
    }
}