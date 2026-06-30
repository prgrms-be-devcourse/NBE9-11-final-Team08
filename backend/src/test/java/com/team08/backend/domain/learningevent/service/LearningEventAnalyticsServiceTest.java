package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.learningevent.dto.ChapterStatsResponse;
import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.dto.CourseStatsResponse;
import com.team08.backend.domain.learningevent.dto.LearningEventResponse;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningEventAnalyticsServiceTest {

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private LearningEventAnalyticsService learningEventAnalyticsService;

    // ──── 강의별 통계 집계 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자가 강의별 통계 조회 성공")
    void getCourseStats_admin_success() {
        Long courseId = 10L;

        given(learningEventRepository.getStatsByCourseId(courseId))
                .willReturn(new CourseStatsProjection(50L, 36000L, 30L));

        CourseStatsResponse stats =
                learningEventAnalyticsService.getCourseStats(99L, courseId, "ROLE_ADMIN");

        assertThat(stats.courseId()).isEqualTo(courseId);
        assertThat(stats.enterCount()).isEqualTo(50L);
        assertThat(stats.watchTimeSeconds()).isEqualTo(36000L);
        assertThat(stats.completionCount()).isEqualTo(30L);
        // 단일 쿼리로 집계됐는지 확인
        verify(learningEventRepository).getStatsByCourseId(courseId);
    }

    @Test
    @DisplayName("강좌 소유 판매자가 강의별 통계 조회 성공")
    void getCourseStats_courseOwner_success() {
        Long sellerId = 5L;
        Long courseId = 10L;
        Course course = mockCourse(courseId, sellerId);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(learningEventRepository.getStatsByCourseId(courseId))
                .willReturn(new CourseStatsProjection(5L, 1000L, 3L));

        CourseStatsResponse stats =
                learningEventAnalyticsService.getCourseStats(sellerId, courseId, "ROLE_SELLER");

        assertThat(stats.courseId()).isEqualTo(courseId);
    }

    @Test
    @DisplayName("강좌 소유자가 아닌 판매자는 통계 조회 불가")
    void getCourseStats_nonOwnerSeller_accessDenied() {
        Long otherId = 9L;
        Long courseId = 10L;
        Course course = mockCourse(courseId, 5L);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() ->
                learningEventAnalyticsService.getCourseStats(otherId, courseId, "ROLE_SELLER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("일반 유저는 강의별 통계 조회 불가")
    void getCourseStats_regularUser_accessDenied() {
        Long courseId = 10L;
        Course course = mockCourse(courseId, 5L);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() ->
                learningEventAnalyticsService.getCourseStats(1L, courseId, "ROLE_USER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);
    }

    // ──── 챕터별 통계 집계 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자가 챕터별 통계 조회 성공")
    void getChapterStats_admin_success() {
        Long chapterId = 20L;

        given(learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_ENTER)).willReturn(40L);
        given(learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_COMPLETE)).willReturn(20L);
        given(learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)).willReturn(600.0);

        ChapterStatsResponse stats =
                learningEventAnalyticsService.getChapterStats(99L, chapterId, "ROLE_ADMIN");

        assertThat(stats.chapterId()).isEqualTo(chapterId);
        assertThat(stats.enterCount()).isEqualTo(40L);
        assertThat(stats.completionCount()).isEqualTo(20L);
        assertThat(stats.avgWatchTimeSeconds()).isEqualTo(600L);
    }

    @Test
    @DisplayName("판매자가 챕터별 통계 조회 성공")
    void getChapterStats_seller_success() {
        Long chapterId = 20L;

        given(learningEventRepository.countByChapterIdAndEventType(eq(chapterId), any())).willReturn(5L);
        given(learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)).willReturn(300.0);

        ChapterStatsResponse stats =
                learningEventAnalyticsService.getChapterStats(5L, chapterId, "ROLE_SELLER");

        assertThat(stats.chapterId()).isEqualTo(chapterId);
    }

    @Test
    @DisplayName("일반 유저는 챕터별 통계 조회 불가")
    void getChapterStats_regularUser_accessDenied() {
        assertThatThrownBy(() ->
                learningEventAnalyticsService.getChapterStats(1L, 20L, "ROLE_USER"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(learningEventRepository, never()).countByChapterIdAndEventType(anyLong(), any());
    }

    // ──── 관리자 전체 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("관리자 전체 이벤트 조회 성공")
    void getAllEvents_admin_success() {
        Pageable pageable = PageRequest.of(0, 50);
        given(learningEventRepository.findAll(pageable)).willReturn(Page.empty());

        Page<LearningEventResponse> result =
                learningEventAnalyticsService.getAllEvents("ROLE_ADMIN", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository).findAll(pageable);
    }

    @Test
    @DisplayName("비관리자는 전체 이벤트 조회 불가")
    void getAllEvents_nonAdmin_accessDenied() {
        assertThatThrownBy(() ->
                learningEventAnalyticsService.getAllEvents("ROLE_SELLER", PageRequest.of(0, 50)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(learningEventRepository, never()).findAll(any(Pageable.class));
    }

    // ──── 판매자 강좌 필터링 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("판매자가 자신의 강좌 이벤트 조회 성공")
    void getSellerEvents_seller_success() {
        Long sellerId = 5L;
        Pageable pageable = PageRequest.of(0, 20);
        List<Long> courseIds = List.of(10L, 11L);

        given(courseRepository.findIdsByInstructorId(sellerId)).willReturn(courseIds);
        given(learningEventRepository.findByCourseIdIn(courseIds, pageable))
                .willReturn(Page.empty());

        Page<LearningEventResponse> result =
                learningEventAnalyticsService.getSellerEvents(sellerId, "ROLE_SELLER", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository).findByCourseIdIn(courseIds, pageable);
    }

    @Test
    @DisplayName("판매자 강좌가 없으면 빈 페이지 반환")
    void getSellerEvents_noCourses_returnsEmpty() {
        Long sellerId = 5L;
        Pageable pageable = PageRequest.of(0, 20);

        given(courseRepository.findIdsByInstructorId(sellerId)).willReturn(List.of());

        Page<LearningEventResponse> result =
                learningEventAnalyticsService.getSellerEvents(sellerId, "ROLE_SELLER", pageable);

        assertThat(result).isEmpty();
        verify(learningEventRepository, never()).findByCourseIdIn(any(), any());
    }

    @Test
    @DisplayName("일반 유저는 판매자 이벤트 조회 불가")
    void getSellerEvents_regularUser_accessDenied() {
        assertThatThrownBy(() ->
                learningEventAnalyticsService.getSellerEvents(1L, "ROLE_USER", PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEARNING_EVENT_ACCESS_DENIED);

        verify(courseRepository, never()).findIdsByInstructorId(anyLong());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixtures
    // ─────────────────────────────────────────────────────────────────────────

    private Course mockCourse(Long courseId, Long instructorId) {
        // 리팩토링 포인트: 빌더 및 public 생성자 제거 대응으로 createDraft 정적 팩토리 메서드 도입
        Course course = Course.createDraft(
                instructorId,
                1L,
                "테스트 강좌",
                "설명",
                "thumbnail.jpg",
                50000
        );
        // 테스트 시나리오 충족을 위해 status 상태 필드를 리플렉션으로 강제 전이 제어
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
