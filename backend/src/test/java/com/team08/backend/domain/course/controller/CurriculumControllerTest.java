package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.ChapterReorderRequest;
import com.team08.backend.domain.course.dto.LectureReorderRequest;
import com.team08.backend.domain.course.service.CurriculumService;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurriculumController.class)
class CurriculumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurriculumService curriculumService;

    private void setMockUser() {
        LoginUserPrincipal principal = mock(LoginUserPrincipal.class);
        LoginUserDto loginUserDto = mock(LoginUserDto.class);

        when(principal.user()).thenReturn(loginUserDto);
        when(loginUserDto.id()).thenReturn(100L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, new ArrayList<>())
        );
    }

    @Test
    void 강좌_내_챕터_순서가_정상적으로_일괄_변경된다() throws Exception {
        setMockUser();
        Long courseId = 1L;
        ChapterReorderRequest request = new ChapterReorderRequest(List.of(
                new ChapterReorderRequest.ChapterOrderElement(10L, 1)
        ));

        mockMvc.perform(put("/api/curriculums/courses/{courseId}/chapters/reorder", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 챕터_내_강의_순서가_정상적으로_일괄_변경된다() throws Exception {
        setMockUser();
        Long chapterId = 10L;
        LectureReorderRequest request = new LectureReorderRequest(List.of(
                new LectureReorderRequest.LectureOrderElement(100L, 1)
        ));

        mockMvc.perform(put("/api/curriculums/chapters/{chapterId}/lectures/reorder", chapterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}