package com.team08.backend.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.study.dto.response.StudyDetailResponse;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyService studyService;

    @Test
    @WithMockLoginUser()
    void 내_스터디_목록을_조회한다() throws Exception {
        // given
        Long userId = 1L;

        List<StudySummaryResponse> response = List.of(
                new StudySummaryResponse(1L, "스터디1", "스터디1", "강사1", 80, 8, 10),
                new StudySummaryResponse(2L, "스터디2", "스터디2", "강사2", 0, 0, 0)
        );

        given(studyService.getMyStudies(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/studies/me"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        objectMapper.writeValueAsString(response)
                ));

        then(studyService).should().getMyStudies(userId);
    }

    @Test
    @WithMockLoginUser()
    void studyId로_스터디_상세를_조회한다() throws Exception {
        Long userId = 1L;
        Long studyId = 10L;
        StudyDetailResponse response = new StudyDetailResponse(
                studyId,
                20L,
                "스터디 제목",
                "스터디 설명",
                StudyStatus.ACTIVE,
                "스터디장",
                StudyMemberRole.MEMBER
        );

        given(studyService.getStudyDetail(studyId, userId)).willReturn(response);

        mockMvc.perform(get("/api/studies/{studyId}", studyId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(studyService).should().getStudyDetail(studyId, userId);
    }

    @Test
    void 비로그인_사용자도_studyId로_스터디_상세를_조회한다() throws Exception {
        Long studyId = 10L;
        StudyDetailResponse response = new StudyDetailResponse(
                studyId,
                20L,
                "스터디 제목",
                "스터디 설명",
                StudyStatus.ACTIVE,
                "스터디장",
                null
        );

        given(studyService.getStudyDetail(studyId, null)).willReturn(response);

        mockMvc.perform(get("/api/studies/{studyId}", studyId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(studyService).should().getStudyDetail(studyId, null);
    }

    @Test
    void courseId로_스터디_id를_조회한다() throws Exception {
        Long courseId = 20L;
        Long studyId = 10L;

        given(studyService.getStudyIdByCourseId(courseId)).willReturn(studyId);

        mockMvc.perform(get("/api/studies/by-course/{courseId}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId));

        then(studyService).should().getStudyIdByCourseId(courseId);
    }
}
