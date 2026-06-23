package com.team08.backend.domain.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.chapter.dto.ChapterInfoResponse;
import com.team08.backend.domain.course.dto.*;
import com.team08.backend.domain.course.entity.CourseSortType;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.service.CourseService;
import com.team08.backend.domain.lecture.dto.LectureInfoResponse;
import com.team08.backend.support.security.WithMockLoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_유효한_데이터로_강좌_생성_요청_시_201_상태코드와_ID를_반환한다() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest(
                "스프링 부트 완벽 가이드",
                "백엔드 개발자를 위한 강의",
                5L,
                30000,
                "images/thumb.jpg"
        );

        given(courseService.createCourse(eq(1L), any(CourseCreateRequest.class))).willReturn(55L);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(55L));
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 강좌_ID로_상세_조회_요청_시_인증이_없어도_200_상태코드와_계층형_커리큘럼_데이터를_반환한다() throws Exception {
        Long courseId = 100L;

        LectureInfoResponse lectureResponse = new LectureInfoResponse(
                10L, "무료 맛보기 강의", "videos/free.m3u8", UUID.randomUUID().toString(), 600, 1, true
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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
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
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_강좌_심사_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).requestCourseReview(eq(courseId), any(Long.class));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비인증_사용자가_강좌_심사_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_강좌_심사_취소_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).cancelCourseReview(eq(courseId), any(Long.class));

        mockMvc.perform(delete("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비인증_사용자가_강좌_심사_취소_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(delete("/api/courses/{courseId}/reviews", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_강좌_판매_중지_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).closeCourse(eq(courseId), any(Long.class));

        mockMvc.perform(post("/api/courses/{courseId}/closing", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비인증_사용자가_강좌_판매_중지_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(post("/api/courses/{courseId}/closing", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_강좌_삭제_요청_시_204_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        doNothing().when(courseService).deleteCourseByInstructor(eq(courseId), eq(1L));

        mockMvc.perform(delete("/api/courses/{courseId}", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void 비인증_사용자가_강좌_삭제_요청_시_401_상태코드를_반환한다() throws Exception {
        Long courseId = 100L;

        mockMvc.perform(delete("/api/courses/{courseId}", courseId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginUser(id = 1L, role = "ROLE_SELLER")
    void 인증된_판매자가_비디오_파일_업로드_및_인코딩_요청_시_202_상태코드를_반환한다() throws Exception {
        Long lectureId = 10L;
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "mock video content".getBytes()
        );

        doNothing().when(courseService).uploadAndEncodeLectureVideo(eq(1L), eq(lectureId), any());

        mockMvc.perform(multipart("/api/courses/lectures/{lectureId}/videos", lectureId)
                        .file(mockFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-access-token")
                        .with(csrf()))
                .andExpect(status().isAccepted());
    }

    @Test
    void 비인증_사용자가_비디오_파일_업로드_요청_시_401_상태코드를_반환한다() throws Exception {
        Long lectureId = 10L;
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "mock video content".getBytes()
        );

        mockMvc.perform(multipart("/api/courses/lectures/{lectureId}/videos", lectureId)
                        .file(mockFile)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}