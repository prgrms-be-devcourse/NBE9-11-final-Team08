package com.team08.backend.domain.lecture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lecture.dto.LectureCreateRequest;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.media.service.VideoAccessService;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(LectureController.class)
@AutoConfigureMockMvc(addFilters = false)
class LectureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LectureService lectureService;

    @MockitoBean
    private VideoAccessService videoAccessService;

    @Test
    void 유효한_데이터로_강의_생성_요청_시_201_상태코드와_ID를_반환한다() throws Exception {
        Long courseId = 10L;
        Long chapterId = 1L;
        LectureCreateRequest request = new LectureCreateRequest(
                "객체지향과 스프링",
                "videos/oop.m3u8",
                UUID.randomUUID().toString(),
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

    @Test
    @WithMockLoginUser
    void 강의_영상_스트리밍_주소_요청_시_200_상태코드와_m3u8_주소를_반환한다() throws Exception {
        Long courseId = 10L;
        Long chapterId = 1L;
        Long lectureId = 50L;
        Long userId = 1L;
        String expectedUrl = "https://cdn.com/lectures/50/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";

        ResponseCookie[] mockCookies = new ResponseCookie[]{
                ResponseCookie.from("CloudFront-Policy", "dummy").build()
        };

        VideoStreamResponse mockResponse = new VideoStreamResponse(expectedUrl, List.of(mockCookies));
        given(videoAccessService.verifyAndGenerateStreamCookies(eq(lectureId), eq(userId))).willReturn(mockResponse);

        mockMvc.perform(get("/api/courses/{courseId}/chapters/{chapterId}/lectures/{lectureId}/stream", courseId, chapterId, lectureId))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));
    }
}