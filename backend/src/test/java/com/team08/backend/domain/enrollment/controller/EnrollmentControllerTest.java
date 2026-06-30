package com.team08.backend.domain.enrollment.controller;

import com.team08.backend.domain.enrollment.dto.EnrolledCourseResponse;
import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
    void 내_active_수강_강좌_목록을_조회한다() throws Exception {
        Long userId = 1L;
        EnrolledCourseResponse course = new EnrolledCourseResponse(
                100L,
                10L,
                20L,
                "스프링 부트 실전",
                "강사",
                "/courses/spring.png",
                50,
                5,
                10,
                LocalDateTime.of(2026, 6, 12, 20, 0)
        );

        given(enrollmentQueryService.getMyActiveCourses(userId)).willReturn(List.of(course));

        mockMvc.perform(get("/api/enrollments/me/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(100L))
                .andExpect(jsonPath("$[0].courseId").value(10L))
                .andExpect(jsonPath("$[0].studyId").value(20L))
                .andExpect(jsonPath("$[0].title").value("스프링 부트 실전"))
                .andExpect(jsonPath("$[0].progressRate").value(50));

        then(enrollmentQueryService).should().getMyActiveCourses(userId);
    }

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
