package com.team08.backend.domain.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.book.dto.BookCreateRequest;
import com.team08.backend.domain.book.dto.BookUpdateRequest;
import com.team08.backend.domain.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @Test
    @WithUserDetails
    void 도서등록_성공시_201_반환() throws Exception {
        BookCreateRequest request = new BookCreateRequest(10L, "자바 마스터", "도훈", "한빛", "설명", 30000, true);
        given(bookService.createBook(any(BookCreateRequest.class), any(Long.class))).willReturn(100L);

        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("100"));
    }

    @Test
    @WithUserDetails
    void 도서등록_실패_유효성검증오류() throws Exception {
        BookCreateRequest request = new BookCreateRequest(null, "", "도훈", "한빛", "설명", 30000, true);

        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails
    void 도서수정_성공시_204_반환() throws Exception {
        BookUpdateRequest request = new BookUpdateRequest(10L, "수정제목", "저자", "출판사", "설명", 20000, true);

        mockMvc.perform(put("/api/books/100")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails
    void 도서삭제_성공시_204_반환() throws Exception {
        mockMvc.perform(delete("/api/books/100")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}