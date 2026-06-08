package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.CurriculumSaveRequest;
import com.team08.backend.domain.course.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @Test
    @WithUserDetails
    void 강의등록_성공시_201_반환() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(10L, "스프링 마스터", "설명", "thumb.png", 50000);
        given(courseService.createCourse(any(CourseCreateRequest.class), any(Long.class))).willReturn(100L);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("100"));
    }

    @Test
    @WithUserDetails
    void 강의등록_실패_유효성검증오류() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(null, "", "설명", "thumb.png", 50000);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails
    void 강의수정_성공시_204_반환() throws Exception {
        CourseUpdateRequest request = new CourseUpdateRequest(10L, "수정제목", "설명", "thumb.png", 40000);

        mockMvc.perform(put("/api/courses/100")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails
    void 강의삭제_성공시_204_반환() throws Exception {
        mockMvc.perform(delete("/api/courses/100")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails
    void 커리큘럼저장_성공시_200_반환() throws Exception {
        CurriculumSaveRequest request = new CurriculumSaveRequest(new ArrayList<>());

        mockMvc.perform(post("/api/courses/100/curriculum")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}