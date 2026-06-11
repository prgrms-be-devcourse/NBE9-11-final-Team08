package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @Test
    void 인증된_판매자가_유효한_데이터로_강좌_생성_요청_시_21_상태코드와_ID를_반환한다() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "스프링 부트 완벽 가이드",
                "백엔드 개발자를 위한 강의",
                5L,
                30000,
                "images/thumb.jpg",
                CourseStatus.DRAFT
        );

        given(courseService.createCourse(eq(1L), any(CourseCreateRequest.class))).willReturn(55L);

        mockMvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token") // 여기 수정
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("요청 성공"))
                .andExpect(jsonPath("$.result").value(55L));
    }
}