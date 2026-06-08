package com.team08.backend.domain.book.controller;

import com.team08.backend.domain.book.dto.BookCreateRequest;
import com.team08.backend.domain.book.dto.BookDetailResponse;
import com.team08.backend.domain.book.dto.BookUpdateRequest;
import com.team08.backend.domain.book.service.BookService;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<Long> createBook(
            @RequestBody BookCreateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request, principal.user().id()));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookDetail(bookId));
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<Void> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookUpdateRequest request,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        bookService.updateBook(bookId, request, principal.user().id());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal LoginUserPrincipal principal
    ) {
        bookService.deleteBook(bookId, principal.user().id());
        return ResponseEntity.noContent().build();
    }
}