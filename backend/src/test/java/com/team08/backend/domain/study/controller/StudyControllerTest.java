package com.team08.backend.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.study.dto.response.StudySummaryResponse;
import com.team08.backend.domain.study.service.StudyService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                new StudySummaryResponse(1L, "스터디1", "스터디1", "강사1"),
                new StudySummaryResponse(2L, "스터디2", "스터디2", "강사2")
        );

        given(studyService.getMyStudies(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/studies/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        objectMapper.writeValueAsString(response)
                ));

        then(studyService).should().getMyStudies(userId);
    }
}
