package com.team08.backend.domain.learningevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
                OffsetDateTime.of(2026, 6, 13, 10, 0, 0, 0, ZoneOffset.ofHours(9)),
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
                  "lectureId": 10,
                  "eventTime": "2026-06-13T10:00:00+09:00"
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
    // GET /api/learning-events/courses/{courseId}/stats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 99L, role = "ROLE_ADMIN")
    @DisplayName("관리자 강의별 통계 조회 - 200 반환")
    void getCourseStats_admin_returns200() throws Exception {
        CourseStatsResponse stats = new CourseStatsResponse(10L, 50L, 36000L, 30L);

        given(learningEventService.getCourseStats(eq(99L), eq(10L), eq("ROLE_ADMIN")))
                .willReturn(stats);

        mockMvc.perform(get("/api/learning-events/courses/10/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(10L))
                .andExpect(jsonPath("$.enterCount").value(50L))
                .andExpect(jsonPath("$.watchTimeSeconds").value(36000L))
                .andExpect(jsonPath("$.completionCount").value(30L));
    }

    @Test
    @WithMockLoginUser(id = 5L, role = "ROLE_SELLER")
    @DisplayName("강좌 소유 판매자 통계 조회 - 200 반환")
    void getCourseStats_seller_returns200() throws Exception {
        CourseStatsResponse stats = new CourseStatsResponse(10L, 20L, 5000L, 15L);

        given(learningEventService.getCourseStats(eq(5L), eq(10L), eq("ROLE_SELLER")))
                .willReturn(stats);

        mockMvc.perform(get("/api/learning-events/courses/10/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(10L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/learning-events/chapters/{chapterId}/stats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 99L, role = "ROLE_ADMIN")
    @DisplayName("관리자 챕터별 통계 조회 - 200 반환")
    void getChapterStats_admin_returns200() throws Exception {
        ChapterStatsResponse stats = new ChapterStatsResponse(20L, 40L, 20L, 600L);

        given(learningEventService.getChapterStats(eq(99L), eq(20L), eq("ROLE_ADMIN")))
                .willReturn(stats);

        mockMvc.perform(get("/api/learning-events/chapters/20/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterId").value(20L))
                .andExpect(jsonPath("$.enterCount").value(40L))
                .andExpect(jsonPath("$.completionCount").value(20L))
                .andExpect(jsonPath("$.avgWatchTimeSeconds").value(600L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/learning-events/admin
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 99L, role = "ROLE_ADMIN")
    @DisplayName("관리자 전체 이벤트 조회 - 200 반환")
    void getAllEvents_admin_returns200() throws Exception {
        given(learningEventService.getAllEvents(eq("ROLE_ADMIN"), any(Pageable.class)))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/learning-events/admin"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/learning-events/seller
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 5L, role = "ROLE_SELLER")
    @DisplayName("판매자 강좌 이벤트 조회 - 200 반환")
    void getSellerEvents_seller_returns200() throws Exception {
        LearningEventResponse event = sampleEventResponse(200L);
        Page<LearningEventResponse> page = new PageImpl<>(List.of(event));

        given(learningEventService.getSellerEvents(eq(5L), eq("ROLE_SELLER"), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/learning-events/seller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(200L));
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
