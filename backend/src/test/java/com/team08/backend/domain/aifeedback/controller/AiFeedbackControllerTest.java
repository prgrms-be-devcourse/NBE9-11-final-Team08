package com.team08.backend.domain.aifeedback.controller;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedbackStatus;
import com.team08.backend.domain.aifeedback.service.AiFeedbackService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiFeedbackService aiFeedbackService;

    @Test
    @WithMockLoginUser
    void 작성자가_AI_피드백을_생성한다() throws Exception {
        AiFeedbackResponse response = response();
        given(aiFeedbackService.generate(10L, 100L, 1L)).willReturn(response);

        mockMvc.perform(post(
                        "/api/studies/{studyId}/activities/{activityId}/ai-feedback",
                        10L,
                        100L
                ).header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId").value(1000L))
                .andExpect(jsonPath("$.studyActivityId").value(100L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.summary").value("핵심 내용을 잘 정리했습니다."))
                .andExpect(jsonPath("$.result.strengths[0]").value("구체적으로 작성했습니다."))
                .andExpect(jsonPath("$.result.improvements[0]").value("근거를 보강해 보세요."))
                .andExpect(jsonPath("$.result.nextSteps[0]").value("예제를 직접 작성해 보세요."));

        then(aiFeedbackService).should().generate(10L, 100L, 1L);
    }

    @Test
    @WithMockLoginUser
    void ACTIVE_스터디_멤버가_AI_피드백을_조회한다() throws Exception {
        given(aiFeedbackService.get(10L, 100L, 1L)).willReturn(response());

        mockMvc.perform(get(
                        "/api/studies/{studyId}/activities/{activityId}/ai-feedback",
                        10L,
                        100L
                ).header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelName").value("stub"))
                .andExpect(jsonPath("$.promptVersion").value("v1"));

        then(aiFeedbackService).should().get(10L, 100L, 1L);
    }

    private AiFeedbackResponse response() {
        return new AiFeedbackResponse(
                1000L,
                100L,
                AiFeedbackStatus.COMPLETED,
                new StructuredFeedback(
                        "핵심 내용을 잘 정리했습니다.",
                        List.of("구체적으로 작성했습니다."),
                        List.of("근거를 보강해 보세요."),
                        List.of("예제를 직접 작성해 보세요.")
                ),
                "stub",
                "v1",
                LocalDateTime.of(2026, 6, 13, 10, 0),
                LocalDateTime.of(2026, 6, 13, 10, 0)
        );
    }
}
