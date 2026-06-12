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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
