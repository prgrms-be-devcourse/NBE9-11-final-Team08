package com.team08.backend.domain.study.controller;

import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.global.auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
@Import(SecurityConfig.class)
public class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyService studyService;

    @Test
    void 내_스터디_목록을_조회한다() throws Exception {
        // given
        Long userId = 1L;

        List<StudySummaryResponse> responses = List.of(
                new StudySummaryResponse(1L, "스터디1", "스터디1", "강사1"),
                new StudySummaryResponse(2L, "스터디2", "스터디2", "강사2")
        );

        given(studyService.getMyStudies(userId)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/studies/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studyId").value(1L))
                .andExpect(jsonPath("$[0].title").value("스터디1"))
                .andExpect(jsonPath("$[0].description").value("스터디1"))
                .andExpect(jsonPath("$[0].ownerNickname").value("강사1"))
                .andExpect(jsonPath("$[1].studyId").value(2L))
                .andExpect(jsonPath("$[1].title").value("스터디2"))
                .andExpect(jsonPath("$[1].description").value("스터디2"))
                .andExpect(jsonPath("$[1].ownerNickname").value("강사2"));

        then(studyService).should().getMyStudies(userId);
    }
}
