package com.team08.backend.domain.enrollment.controller;

import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentQueryService enrollmentQueryService;

    @Test
    @WithMockLoginUser(id = 1L)
    void 로그인_사용자와_courseId로_active_enrollment_존재_여부를_조회한다() throws Exception {
        Long userId = 1L;
        Long courseId = 10L;

        given(enrollmentQueryService.hasActiveEnrollment(userId, courseId)).willReturn(true);

        mockMvc.perform(get("/api/enrollments/courses/{courseId}/active", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        then(enrollmentQueryService).should().hasActiveEnrollment(userId, courseId);
    }
}
