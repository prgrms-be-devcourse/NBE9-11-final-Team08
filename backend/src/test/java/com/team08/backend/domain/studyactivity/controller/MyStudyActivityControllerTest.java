package com.team08.backend.domain.studyactivity.controller;

import com.team08.backend.domain.studyactivity.dto.StudyActivityResponse;
import com.team08.backend.domain.studyactivity.service.StudyActivityService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyStudyActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class MyStudyActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyActivityService studyActivityService;

    @Test
    @WithMockLoginUser(id = 1L)
    void 내_스터디_활동_목록을_조회한다() throws Exception {
        Long userId = 1L;
        StudyActivityResponse activity = new StudyActivityResponse(
                100L,
                10L,
                userId,
                "테스터",
                "오늘 학습한 내용을 스터디원들과 공유합니다.",
                LocalDateTime.of(2026, 6, 12, 20, 0)
        );

        given(studyActivityService.getMyActivities(eq(userId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(activity)));

        mockMvc.perform(get("/api/study-activities/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activityId").value(100L))
                .andExpect(jsonPath("$.content[0].studyId").value(10L))
                .andExpect(jsonPath("$.content[0].authorId").value(userId));

        then(studyActivityService).should().getMyActivities(eq(userId), any(Pageable.class));
    }
}
