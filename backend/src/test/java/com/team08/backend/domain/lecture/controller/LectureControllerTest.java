package com.team08.backend.domain.lecture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.lecture.service.LectureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LectureController.class)
@AutoConfigureMockMvc(addFilters = false)
class LectureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LectureService lectureService;

    @Test
    void 유효한_데이터로_강의_생성_요청_시_201_상태코드와_ID를_반환한다() throws Exception {
        Long courseId = 10L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "객체지향과 스프링",
                "videos/oop.m3u8",
                "스프링이 사랑한 오대 객체지향 원칙",
                600,
                1,
                true
        );

        given(lectureService.createLecture(eq(courseId), eq(chapterId), any(LectureCreateRequest.class))).willReturn(50L);

        mockMvc.perform(post("/api/courses/{courseId}/chapters/{chapterId}/lectures", courseId, chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(50L));
    }
}
