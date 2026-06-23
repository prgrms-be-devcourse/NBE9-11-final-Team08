package com.team08.backend.domain.lectureqna.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerRequest;
import com.team08.backend.domain.lectureqna.dto.QnaAnswerResponse;
import com.team08.backend.domain.lectureqna.service.QnaAnswerService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QnaAnswerController.class)
@AutoConfigureMockMvc(addFilters = false)
class QnaAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QnaAnswerService qnaAnswerService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/qna/questions/{questionId}/answers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 10L, role = "ROLE_SELLER")
    @DisplayName("QnA 답변 작성 - 201 반환")
    void createAnswer_returns201() throws Exception {
        QnaAnswerRequest request = new QnaAnswerRequest("답변 내용");
        given(qnaAnswerService.createAnswer(eq(100L), eq(10L), eq("답변 내용")))
                .willReturn(sampleAnswer(200L));

        mockMvc.perform(post("/api/qna/questions/100/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(200L))
                .andExpect(jsonPath("$.questionId").value(100L));
    }

    @Test
    @WithMockLoginUser(id = 10L, role = "ROLE_SELLER")
    @DisplayName("content 누락 시 400 반환")
    void createAnswer_missingContent_returns400() throws Exception {
        QnaAnswerRequest request = new QnaAnswerRequest(null);

        mockMvc.perform(post("/api/qna/questions/100/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qnaAnswerService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/qna/questions/{questionId}/answers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 10L, role = "ROLE_SELLER")
    @DisplayName("QnA 답변 수정 - 200 반환")
    void updateAnswer_returns200() throws Exception {
        QnaAnswerRequest request = new QnaAnswerRequest("수정 답변");
        given(qnaAnswerService.updateAnswer(eq(100L), eq(10L), eq("수정 답변")))
                .willReturn(sampleAnswer(200L));

        mockMvc.perform(put("/api/qna/questions/100/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/qna/questions/{questionId}/answers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 10L, role = "ROLE_SELLER")
    @DisplayName("QnA 답변 삭제 - 204 반환")
    void deleteAnswer_returns204() throws Exception {
        mockMvc.perform(delete("/api/qna/questions/100/answers"))
                .andExpect(status().isNoContent());

        verify(qnaAnswerService).deleteAnswer(100L, 10L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private QnaAnswerResponse sampleAnswer(Long id) {
        return new QnaAnswerResponse(
                id, 100L, 10L, "답변 내용",
                LocalDateTime.of(2026, 6, 13, 11, 0),
                LocalDateTime.of(2026, 6, 13, 11, 0)
        );
    }
}
