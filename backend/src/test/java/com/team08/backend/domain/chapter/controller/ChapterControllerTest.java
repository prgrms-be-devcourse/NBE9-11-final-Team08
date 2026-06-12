package com.team08.backend.domain.chapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.service.ChapterService;
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

@WebMvcTest(ChapterController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChapterService chapterService;

    @Test
    void 유효한_데이터로_챕터_생성_요청_시_201_상태코드와_ID를_반환한다() throws Exception {
        Long courseId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("기본 문법 마스터", 1);

        given(chapterService.createChapter(eq(courseId), any(ChapterCreateRequest.class))).willReturn(10L);

        mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(10L));
    }
}
