package com.team08.backend.domain.learningevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.dto.RecordLearningEventRequest;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.service.LearningEventService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class LearningEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LearningEventService learningEventService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/learning-events
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("학습 이벤트 기록 - 201 반환")
    void recordEvent_returns201() throws Exception {
        RecordLearningEventRequest request = new RecordLearningEventRequest(
                1L, 2L, 10L,
                LearningEventType.LECTURE_ENTER,
                null,
                "event-key-abc"
        );
        LearningEventResponse response = new LearningEventResponse(
                100L, 1L, 1L, 2L, 10L,
                LearningEventType.LECTURE_ENTER, null,
                LocalDateTime.of(2026, 6, 13, 10, 0)
        );

        given(learningEventService.recordEvent(eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/learning-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.eventType").value("LECTURE_ENTER"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithMockLoginUser
    @DisplayName("필수 필드 누락 시 400 반환")
    void recordEvent_missingRequiredField_returns400() throws Exception {
        // eventType 누락
        String invalidBody = """
                {
                  "lectureId": 10
                }
                """;

        mockMvc.perform(post("/api/learning-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(learningEventService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/learning-events/users/{userId}/activities
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_USER")
    @DisplayName("본인 활동 목록 조회 - 200 반환")
    void getUserActivities_self_returns200() throws Exception {
        LearningEventResponse event = sampleEventResponse(1L);
        Page<LearningEventResponse> page = new PageImpl<>(List.of(event));

        given(learningEventService.getUserActivities(eq(1L), eq(1L), eq("ROLE_USER"), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/learning-events/users/1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockLoginUser(id = 99L, role = "ROLE_ADMIN")
    @DisplayName("관리자가 타 사용자 활동 조회 - 200 반환")
    void getUserActivities_admin_returns200() throws Exception {
        given(learningEventService.getUserActivities(eq(99L), eq(1L), eq("ROLE_ADMIN"), any(Pageable.class)))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/learning-events/users/1/activities"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private LearningEventResponse sampleEventResponse(Long id) {
        return new LearningEventResponse(
                id, 1L, 1L, 2L, 10L,
                LearningEventType.LECTURE_ENTER, null,
                LocalDateTime.of(2026, 6, 13, 10, 0)
        );
    }
}
