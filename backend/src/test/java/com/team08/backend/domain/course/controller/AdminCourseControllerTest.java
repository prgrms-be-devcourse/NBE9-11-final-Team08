package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.CourseRejectRequest;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCourseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 인증된_관리자가_강좌_심사_승인_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).approveCourseReview(courseId, 1L);

        mockMvc.perform(post("/api/admin/courses/{courseId}/approve", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 인증된_관리자가_유효한_사유로_강좌_심사_반려_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("콘텐츠 부적절 및 커리큘럼 보완 필요");

        doNothing().when(courseService).rejectCourseReview(courseId, 1L, request.reason());

        mockMvc.perform(post("/api/admin/courses/{courseId}/reject", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 강좌_심사_반려_요청_시_반려_사유가_공백이면_400_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("");

        mockMvc.perform(post("/api/admin/courses/{courseId}/reject", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비인증_사용자가_강좌_심사_승인_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(post("/api/admin/courses/{courseId}/approve", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비인증_사용자가_강좌_심사_반려_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("콘텐츠 부적절");

        mockMvc.perform(post("/api/admin/courses/{courseId}/reject", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 인증된_관리자가_유효한_사유로_강좌_강제_판매_중지_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("운영 정책 위반 및 불법 요소 발견");

        doNothing().when(courseService).suspendCourseByAdmin(courseId, 1L, request.reason());

        mockMvc.perform(post("/api/admin/courses/{courseId}/suspension", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 강좌_강제_판매_중지_요청_시_중지_사유가_공백이면_400_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("");

        mockMvc.perform(post("/api/admin/courses/{courseId}/suspension", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비인증_사용자가_강좌_강제_판매_중지_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseRejectRequest request = new CourseRejectRequest("운영 정책 위반");

        mockMvc.perform(post("/api/admin/courses/{courseId}/suspension", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_ADMIN")
    void 인증된_관리자가_강좌_삭제_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).deleteCourseByAdmin(courseId, 1L);

        mockMvc.perform(delete("/api/admin/courses/{courseId}", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비인증_사용자가_강좌_삭제_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(delete("/api/admin/courses/{courseId}", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}