package com.team08.backend.domain.studyactivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.studyactivity.dto.StudyActivityCreateRequest;
import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.service.StudyActivityService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyActivityService studyActivityService;

    @Test
    @WithMockLoginUser
    void 스터디_활동을_생성한다() throws Exception {
        Long studyId = 10L;
        Long userId = 1L;
        StudyActivityCreateRequest request =
                new StudyActivityCreateRequest("오늘 학습한 내용을 스터디원들과 공유합니다.");
        StudyActivityResponse response = new StudyActivityResponse(
                100L,
                studyId,
                userId,
                request.content(),
                LocalDateTime.of(2026, 6, 12, 20, 0)
        );

        given(studyActivityService.createActivity(studyId, userId, request.content()))
                .willReturn(response);

        mockMvc.perform(post("/api/studies/{studyId}/activities", studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(studyActivityService).should()
                .createActivity(studyId, userId, request.content());
    }

    @Test
    @WithMockLoginUser
    void 활동_내용이_20자보다_짧으면_생성할_수_없다() throws Exception {
        StudyActivityCreateRequest request =
                new StudyActivityCreateRequest("열아홉자보다 짧은 내용");

        mockMvc.perform(post("/api/studies/{studyId}/activities", 10L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studyActivityService);
    }

    @Test
    @WithMockLoginUser
    void 활동_내용이_2000자를_초과하면_생성할_수_없다() throws Exception {
        StudyActivityCreateRequest request =
                new StudyActivityCreateRequest("가".repeat(2001));

        mockMvc.perform(post("/api/studies/{studyId}/activities", 10L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studyActivityService);
    }

    @Test
    @WithMockLoginUser
    void 활동_내용이_공백이면_생성할_수_없다() throws Exception {
        StudyActivityCreateRequest request = new StudyActivityCreateRequest(" ".repeat(20));

        mockMvc.perform(post("/api/studies/{studyId}/activities", 10L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(studyActivityService).should(never())
                .createActivity(10L, 1L, request.content());
    }

    @Test
    @WithMockLoginUser
    void 스터디_활동_목록을_기본_페이지로_조회한다() throws Exception {
        Long studyId = 10L;
        Long userId = 1L;
        StudyActivityResponse activity = new StudyActivityResponse(
                100L,
                studyId,
                userId,
                "오늘 학습한 내용을 스터디원들과 공유합니다.",
                LocalDateTime.of(2026, 6, 12, 20, 0)
        );
        Page<StudyActivityResponse> response = new PageImpl<>(List.of(activity));

        given(studyActivityService.getActivities(eq(studyId), eq(userId), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/studies/{studyId}/activities", studyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activityId").value(100L))
                .andExpect(jsonPath("$.content[0].studyId").value(studyId))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1));

        then(studyActivityService).should().getActivities(
                eq(studyId),
                eq(userId),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 10
                )
        );
    }

    @Test
    @WithMockLoginUser
    void 스터디_활동_상세를_조회한다() throws Exception {
        Long studyId = 10L;
        Long activityId = 100L;
        Long userId = 1L;
        StudyActivityResponse response = new StudyActivityResponse(
                activityId,
                studyId,
                userId,
                "오늘 학습한 내용을 스터디원들과 공유합니다.",
                LocalDateTime.of(2026, 6, 12, 20, 0)
        );

        given(studyActivityService.getActivity(studyId, activityId, userId))
                .willReturn(response);

        mockMvc.perform(get(
                        "/api/studies/{studyId}/activities/{activityId}",
                        studyId,
                        activityId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(studyActivityService).should()
                .getActivity(studyId, activityId, userId);
    }
}
