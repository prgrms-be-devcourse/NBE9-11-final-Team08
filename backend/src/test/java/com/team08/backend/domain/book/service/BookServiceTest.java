package com.team08.backend.domain.book.service;

import com.team08.backend.domain.book.dto.BookCreateRequest;
import com.team08.backend.domain.book.dto.BookDetailResponse;
import com.team08.backend.domain.book.dto.BookUpdateRequest;
import com.team08.backend.domain.book.entity.Book;
import com.team08.backend.domain.book.repository.BookRepository;
import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.instructor.entity.InstructorProfile;
import com.team08.backend.domain.instructor.repository.InstructorProfileRepository;
import com.team08.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @InjectMocks
    private BookService bookService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Test
    void 도서등록_성공() {
        InstructorProfile profile = mock(InstructorProfile.class);
        User seller = mock(User.class);
        given(instructorProfileRepository.existsByUserIdAndApprovedAtIsNotNull(1L)).willReturn(true);
        given(instructorProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(profile.getUser()).willReturn(seller);

        Category category = mock(Category.class);
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category));

        BookCreateRequest request = new BookCreateRequest(10L, "자바 마스터", "도훈", "한빛", "설명", 30000, true);
        Book book = mock(Book.class);
        given(book.getId()).willReturn(100L);
        given(bookRepository.save(any(Book.class))).willReturn(book);

        bookService.createBook(request, 1L);

        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void 도서등록_실패_판매자권한없음() {
        given(instructorProfileRepository.existsByUserIdAndApprovedAtIsNotNull(1L)).willReturn(false);

        BookCreateRequest request = new BookCreateRequest(10L, "자바 마스터", "도훈", "한빛", "설명", 30000, true);

        assertThrows(AccessDeniedException.class, () -> bookService.createBook(request, 1L));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void 도서상세조회_성공() {
        Book book = Book.builder()
                .title("자바 마스터")
                .author("도훈")
                .price(30000)
                .build();
        ReflectionTestUtils.setField(book, "id", 100L);

        given(bookRepository.findById(100L)).willReturn(Optional.of(book));

        BookDetailResponse response = bookService.getBookDetail(100L);

        assertEquals("자바 마스터", response.title());
        assertEquals("도훈", response.author());
    }

    @Test
    void 도서수정_실패_본인아님() {
        User seller = newInstance(User.class);
        ReflectionTestUtils.setField(seller, "id", 1L);

        Book book = Book.builder()
                .seller(seller)
                .title("기존 도서")
                .price(20000)
                .build();

        given(bookRepository.findById(100L)).willReturn(Optional.of(book));
        BookUpdateRequest request = new BookUpdateRequest(10L, "수정 제목", "수정 저자", "수정 출판사", "수정 설명", 25000, true);

        assertThrows(AccessDeniedException.class, () -> bookService.updateBook(100L, request, 2L));
    }

    @Test
    void 도서삭제_실패_본인아님() {
        User seller = newInstance(User.class);
        ReflectionTestUtils.setField(seller, "id", 1L);

        Book book = Book.builder()
                .seller(seller)
                .title("삭제할 도서")
                .price(20000)
                .build();

        given(bookRepository.findById(100L)).willReturn(Optional.of(book));

        assertThrows(AccessDeniedException.class, () -> bookService.deleteBook(100L, 2L));
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test entity.", e);
        }
    }
}