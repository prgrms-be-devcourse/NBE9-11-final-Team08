package com.team08.backend.domain.lecturereflection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lecturereflection.dto.LectureReflectionRequest;
import com.team08.backend.domain.lecturereflection.dto.LectureReflectionResponse;
import com.team08.backend.domain.lecturereflection.service.LectureReflectionService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LectureReflectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class LectureReflectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LectureReflectionService reflectionService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/lectures/{lectureId}/reflections
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("회고 작성 - 201 반환")
    void createReflection_returns201() throws Exception {
        LectureReflectionRequest request = new LectureReflectionRequest("회고 내용");
        given(reflectionService.createReflection(eq(1L), eq(10L), eq("회고 내용")))
                .willReturn(new LectureReflectionResponse(100L, 1L, 10L, "회고 내용"));

        mockMvc.perform(post("/api/lectures/10/reflections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.content").value("회고 내용"));
    }

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("회고 내용이 비어 있으면 400 반환")
    void createReflection_blankContent_returns400() throws Exception {
        LectureReflectionRequest request = new LectureReflectionRequest("");

        mockMvc.perform(post("/api/lectures/10/reflections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reflectionService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/lectures/{lectureId}/reflections/{reflectionId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("회고 수정 - 200 반환")
    void updateReflection_returns200() throws Exception {
        LectureReflectionRequest request = new LectureReflectionRequest("수정 회고");
        given(reflectionService.updateReflection(eq(100L), eq(1L), eq("수정 회고")))
                .willReturn(new LectureReflectionResponse(100L, 1L, 10L, "수정 회고"));

        mockMvc.perform(put("/api/lectures/10/reflections/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정 회고"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/lectures/{lectureId}/reflections
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockLoginUser(id = 1L)
    @DisplayName("회고 조회 - 200 반환")
    void getReflection_returns200() throws Exception {
        given(reflectionService.getReflection(eq(1L), eq(10L)))
                .willReturn(new LectureReflectionResponse(100L, 1L, 10L, "회고 내용"));

        mockMvc.perform(get("/api/lectures/10/reflections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.lectureId").value(10L));
    }
}
