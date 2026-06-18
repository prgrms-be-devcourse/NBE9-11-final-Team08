package com.team08.backend.domain.lectureqna.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerSummary;
import com.team08.backend.domain.lectureqna.dto.QnaQuestionRequest;
import com.team08.backend.domain.lectureqna.dto.QnaQuestionResponse;
import com.team08.backend.domain.lectureqna.service.QnaQuestionService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QnaQuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class QnaQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QnaQuestionService qnaQuestionService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/lectures/{lectureId}/qna
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("강의 QnA 목록 조회 - 200 반환")
    void getQna_returns200() throws Exception {
        Page<QnaQuestionResponse> page = new PageImpl<>(List.of(sampleQuestion(1L)));
        given(qnaQuestionService.getQuestionsNAnswers(eq(5L), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/lectures/5/qna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("제목"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/lectures/{lectureId}/qna/questions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("QnA 질문 작성 - 201 반환")
    void createQuestion_returns201() throws Exception {
        QnaQuestionRequest request = new QnaQuestionRequest("제목", "내용");
        given(qnaQuestionService.createQuestion(eq(5L), eq(1L), eq("제목"), eq("내용")))
                .willReturn(sampleQuestion(100L));

        mockMvc.perform(post("/api/lectures/5/qna/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("제목이 비어 있으면 400 반환")
    void createQuestion_blankTitle_returns400() throws Exception {
        QnaQuestionRequest request = new QnaQuestionRequest("", "내용");

        mockMvc.perform(post("/api/lectures/5/qna/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qnaQuestionService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/qna/questions/{questionId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("QnA 질문 수정 - 200 반환")
    void updateQuestion_returns200() throws Exception {
        QnaQuestionRequest request = new QnaQuestionRequest("수정 제목", "수정 내용");
        given(qnaQuestionService.updateQuestion(eq(100L), eq(1L), eq("수정 제목"), eq("수정 내용")))
                .willReturn(sampleQuestion(100L));

        mockMvc.perform(put("/api/qna/questions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/qna/questions/{questionId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("QnA 질문 삭제 - 204 반환")
    void deleteQuestion_returns204() throws Exception {
        mockMvc.perform(delete("/api/qna/questions/100"))
                .andExpect(status().isNoContent());

        verify(qnaQuestionService).deleteQuestion(100L, 1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private QnaQuestionResponse sampleQuestion(Long id) {
        return new QnaQuestionResponse(
                id, 5L, 1L, "제목", "내용",
                LocalDateTime.of(2026, 6, 13, 10, 0),
                LocalDateTime.of(2026, 6, 13, 10, 0),
                new QnaAnswerSummary(1L, "답변 내용", LocalDateTime.of(2026, 6, 13, 11, 0))
        );
    }
}
