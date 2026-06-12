package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.course.dto.ChapterInfoResponse;
import com.team08.backend.domain.course.dto.CourseCardResponse;
import com.team08.backend.domain.course.dto.CourseCreateRequest;
import com.team08.backend.domain.course.dto.CourseDetailResponse;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.dto.LectureInfoResponse;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.global.auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @WithMockUser
    void 인증된_판매자가_유효한_데이터로_강좌_생성_요청_시_201_상태코드와_ID를_반환한다() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "스프剩 부트 완벽 가이드",
                "백엔드 개발자를 위한 강의",
                5L,
                30000,
                "images/thumb.jpg"
        );

        given(courseService.createCourse(eq(1L), any(CourseCreateRequest.class))).willReturn(55L);

        mockMvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(55L));
    }

    @Test
    @WithMockUser
    void 강좌_ID로_상세_조회_요청_시_인증이_없어도_200_상태코드와_계층형_커리큘럼_데이터를_반환한다() throws Exception {
        Long courseId = 100L;

        LectureInfoResponse lectureResponse = new LectureInfoResponse(
                10L, "무료 맛보기 강의", "videos/free.m3u8", 600, 1, true
        );
        ChapterInfoResponse chapterResponse = new ChapterInfoResponse(
                1L, "첫 번째 챕터", 1, List.of(lectureResponse)
        );
        CourseDetailResponse response = new CourseDetailResponse(
                courseId, 1L, 5L, "스프링 부트 완벽 가이드", "백엔드 강의",
                "images/thumb.jpg", 30000, CourseStatus.ON_SALE, 0, List.of(chapterResponse)
        );

        given(courseService.getCourseDetail(courseId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.title").value("스프링 부트 완벽 가이드"))
                .andExpect(jsonPath("$.chapters[0].id").value(1L))
                .andExpect(jsonPath("$.chapters[0].lectures[0].id").value(10L))
                .andExpect(jsonPath("$.chapters[0].lectures[0].m3u8Path").value("videos/free.m3u8"));
    }

    @Test
    @WithMockUser
    void 강좌_목록_조회_요청_시_정렬_파라미터가_없으면_디폴트로_조회수순_정렬된_강좌_목록을_반환한다() throws Exception {
        CourseCardResponse courseCard = new CourseCardResponse(
                1L, 1L, 5L, "스프링 부트 완벽 가이드", "images/thumb.jpg", 30000, 150
        );
        Page<CourseCardResponse> pagedResponses = new PageImpl<>(List.of(courseCard));

        given(courseService.getCourses(any(CourseSortType.class), any(Pageable.class))).willReturn(pagedResponses);

        mockMvc.perform(get("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].viewCount").value(150));
    }

    @Test
    @WithMockUser
    void 강좌_목록_조회_요청_시_정렬_파라미터를_지정하면_해당_조건으로_정렬된_강좌_목록을_반환한다() throws Exception {
        CourseCardResponse courseCard = new CourseCardResponse(
                1L, 1L, 5L, "스프링 부트 완벽 가이드", "images/thumb.jpg", 30000, 150
        );
        Page<CourseCardResponse> pagedResponses = new PageImpl<>(List.of(courseCard));

        given(courseService.getCourses(any(CourseSortType.class), any(Pageable.class))).willReturn(pagedResponses);

        mockMvc.perform(get("/api/courses")
                        .param("sort", "PRICE_ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].price").value(30000));
    }

    @Test
    @WithMockUser
    void 인증된_판매자가_유효한_데이터로_강좌_일반_정보_수정_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseUpdateRequest.LectureUpdateRequest lectureUpdate = new CourseUpdateRequest.LectureUpdateRequest(20L, "수정 강의", 400, 1, true);
        CourseUpdateRequest.ChapterUpdateRequest chapterUpdate = new CourseUpdateRequest.ChapterUpdateRequest(10L, "수정 챕터", 1, List.of(lectureUpdate));
        CourseUpdateRequest request = new CourseUpdateRequest("수정 제목", "수정 설명", 5L, 50000, "new.png", List.of(chapterUpdate));

        doNothing().when(courseService).updateCourseGeneralInfo(eq(courseId), any(Long.class), any(CourseUpdateRequest.class));

        mockMvc.perform(put("/api/courses/{courseId}", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void 강좌_일반_정보_수정_요청_시_필수_데이터가_누락되면_400_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;
        CourseUpdateRequest request = new CourseUpdateRequest("", "설명", 5L, 50000, "new.png", List.of());

        mockMvc.perform(put("/api/courses/{courseId}", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void 인증된_판매자가_강좌_심사_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).submitCourseReview(eq(courseId), any(Long.class));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void 비인증_사용자가_강좌_심사_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}