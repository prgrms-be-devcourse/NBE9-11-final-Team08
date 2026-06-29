package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.course.entity.CourseStatus;
import org.springframework.test.util.ReflectionTestUtils;
import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecture.access.LectureAccessValidator;
import com.team08.backend.domain.lastwatchedlecture.service.LastWatchedLectureService;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ChapterService chapterService;

    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private LastWatchedLectureService lastWatchedLectureService;
    @Mock
    private LectureService lectureService;
    @Mock
    private LectureAccessValidator lectureAccessValidator;
    @Mock
    private CourseAccessAuthorizer courseAccessAuthorizer;

    // ── 챕터 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("챕터 생성 성공")
    void createChapter_success() {
        Long courseId = 1L;
        Long userId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);
        Course course = TestEntityFactory.course(courseId);

        Chapter savedChapter = ChapterFixture.chapter(10L, request.title(), request.orderNo(), course);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.save(any(Chapter.class))).willReturn(savedChapter);

        Long chapterId = chapterService.createChapter(courseId, userId, request);

        assertThat(chapterId).isEqualTo(10L);
        verify(courseRepository).findById(courseId);
        verify(chapterRepository).save(any(Chapter.class));
        verify(courseAccessAuthorizer).authorizeByCourseId(courseId, userId, CourseAction.MANAGE_COURSE);
    }

    @Test
    @DisplayName("챕터 생성 실패 - 존재하지 않는 강좌로 챕터 생성 시")
    void createChapter_courseNotFound(){
        Long invalidCourseId = 999L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.createChapter(invalidCourseId, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(courseRepository).findById(invalidCourseId);
    }

    @Test
    @DisplayName("챕터 생성 실패 - 이미 ON_SALE 상태인 강좌에 챕터 생성 시")
    void createChapter_alreadyOnSale() {
        Long courseId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);
        Course course = TestEntityFactory.course(courseId);
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> chapterService.createChapter(courseId, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_ALREADY_ON_SALE.getMessage());
    }

    @Test
    @DisplayName("챕터 생성 실패 - 이미 SUSPENDED 상태인 강좌에 챕터 생성 시")
    void createChapter_suspended() {
        Long courseId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);
        Course course = TestEntityFactory.course(courseId);
        ReflectionTestUtils.setField(course, "status", CourseStatus.SUSPENDED);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> chapterService.createChapter(courseId, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_ALREADY_ON_SALE.getMessage());
    }

    // ── 챕터 리스트 조회 ──────────────────────────────────────────────────

    @Test
    @DisplayName("챕터 리스트 조회 성공 - 강의 포함")
    void getChaptersWithLectures_success() {
        Long courseId = 1L;

        Chapter chapter = mock(Chapter.class);
        given(chapter.getId()).willReturn(10L);
        given(chapter.getTitle()).willReturn("챕터1");
        given(chapter.getOrderNo()).willReturn(1);
        given(chapter.getLectures()).willReturn(List.of());

        given(chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(courseId))
                .willReturn(List.of(chapter));

        List<ChapterWithLecturesResponse> result = chapterService.getChaptersWithLectures(courseId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("챕터1");
        assertThat(result.get(0).orderNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("챕터 리스트 조회 - 빈 결과")
    void getChaptersWithLectures_empty() {
        given(chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(any()))
                .willReturn(List.of());

        List<ChapterWithLecturesResponse> result = chapterService.getChaptersWithLectures(99L);

        assertThat(result).isEmpty();
    }

    // ── 강좌 내 최근 수강 강의 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("최근 수강 강의 조회 성공 - last_watched 행 존재 (단건 조회)")
    void getLastWatchedLecture_fromLastWatchedTable() {
        Long courseId = 1L;
        Long userId = 1L;
        Long lectureId = 100L;

        given(lastWatchedLectureService.findLectureId(userId, courseId))
                .willReturn(Optional.of(lectureId));

        Lecture lecture = mockLecture(lectureId, 10L, "강의1", 1);
        LectureProgress progress = mockProgress(userId, lectureId, 300, false);
        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId))
                .willReturn(Optional.of(progress));

        LectureEnterResponse response = chapterService.getLastWatchedLecture(courseId, userId);

        assertThat(response).isNotNull();
        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.progress().lastPositionSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("최근 수강 강의 조회 성공 - last_watched 행이 없으면 진행도 집계로 폴백")
    void getLastWatchedLecture_fallbackToProgress() {
        Long courseId = 1L;
        Long userId = 1L;
        Long lectureId = 100L;

        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(List.of(lectureId));

        LectureProgress progress = mockProgress(userId, lectureId, 300, false);
        given(progress.getLectureId()).willReturn(lectureId);
        given(lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(userId, List.of(lectureId)))
                .willReturn(Optional.of(progress));

        Lecture lecture = mockLecture(lectureId, 10L, "강의1", 1);
        given(lectureRepository.findById(progress.getLectureId())).willReturn(Optional.of(lecture));

        LectureEnterResponse response = chapterService.getLastWatchedLecture(courseId, userId);

        assertThat(response).isNotNull();
        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.progress().lastPositionSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("최근 수강 강의 조회 - 수강 이력 없음 (null 반환)")
    void getLastWatchedLecture_noHistory() {
        Long courseId = 1L;
        Long userId = 1L;

        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(List.of(100L));
        given(lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(eq(userId), any()))
                .willReturn(Optional.empty());

        LectureEnterResponse response = chapterService.getLastWatchedLecture(courseId, userId);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("최근 수강 강의 조회 - 강좌에 강의 없음 (null 반환)")
    void getLastWatchedLecture_noLecturesInCourse() {
        Long courseId = 1L;
        Long userId = 1L;

        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(List.of());

        LectureEnterResponse response = chapterService.getLastWatchedLecture(courseId, userId);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("최근 수강 강의 조회 실패 - 접근 권한 없음")
    void getLastWatchedLecture_accessDenied() {
        Long courseId = 1L;
        Long userId = 1L;

        willThrow(new CustomException(ErrorCode.STUDY_ACCESS_DENIED))
                .given(lectureAccessValidator).validateCourseAccess(courseId, userId);

        assertThatThrownBy(() -> chapterService.getLastWatchedLecture(courseId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);
    }

    // ── 챕터 첫 강의 입장 ──────────────────────────────────────────────────

    @Test
    @DisplayName("챕터 첫 강의 입장 성공 - 학습 이력 있음")
    void enterFirstLecture_success_withProgress() {
        Long courseId = 1L;
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        Chapter chapter = mockChapter();
        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

        Lecture lecture = mockLecture(lectureId, chapterId, "강의1", 1);
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(Optional.of(lecture));

        // 실제 입장은 LectureService 에 위임된다.
        LectureEnterResponse expected = LectureEnterResponse.of(lecture, mockProgress(userId, lectureId, 120, false));
        given(lectureService.enterLecture(courseId, chapterId, lectureId, userId)).willReturn(expected);

        LectureEnterResponse response = chapterService.enterFirstLecture(courseId, chapterId, userId);

        assertThat(response).isSameAs(expected);
        assertThat(response.progress()).isNotNull();
        assertThat(response.progress().lastPositionSeconds()).isEqualTo(120);
        verify(lectureService).enterLecture(courseId, chapterId, lectureId, userId);
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 성공 - 학습 이력 없음 (progress null)")
    void enterFirstLecture_success_withoutProgress() {
        Long courseId = 1L;
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        Chapter chapter = mockChapter();
        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

        Lecture lecture = mockLecture(lectureId, chapterId, "강의1", 1);
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(Optional.of(lecture));

        LectureEnterResponse expected = LectureEnterResponse.of(lecture, null);
        given(lectureService.enterLecture(courseId, chapterId, lectureId, userId)).willReturn(expected);

        LectureEnterResponse response = chapterService.enterFirstLecture(courseId, chapterId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.progress()).isNull();
        verify(lectureService).enterLecture(courseId, chapterId, lectureId, userId);
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 실패 - 챕터 없음")
    void enterFirstLecture_chapterNotFound() {
        given(chapterRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.enterFirstLecture(1L, 10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }

    // 챕터-강좌 정합성(소속) 검증은 enterLecture 로 위임되었으므로
    // LectureServiceTest 에서 CHAPTER_NOT_FOUND/COURSE_NOT_FOUND 로 커버한다.

    @Test
    @DisplayName("챕터 첫 강의 입장 실패 - 챕터에 강의 없음")
    void enterFirstLecture_lectureNotFound() {
        Chapter chapter = mockChapter();
        given(chapterRepository.findById(any())).willReturn(Optional.of(chapter));
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.enterFirstLecture(1L, 10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER);
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 - 수강권 검사는 enterLecture 에 위임되어 예외가 전파된다")
    void enterFirstLecture_delegatesEnrollmentToEnterLecture() {
        Long courseId = 1L;
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        Chapter chapter = mockChapter();
        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
        Lecture lecture = mock(Lecture.class);
        given(lecture.getId()).willReturn(lectureId);
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(Optional.of(lecture));
        given(lectureService.enterLecture(courseId, chapterId, lectureId, userId))
                .willThrow(new CustomException(ErrorCode.ENROLLMENT_ACCESS_DENIED));

        assertThatThrownBy(() -> chapterService.enterFirstLecture(courseId, chapterId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }


    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    // enterFirstLecture 는 챕터 존재 여부만 확인하고 정합성/수강권 검증은 enterLecture 에 위임하므로
    // 챕터 mock 은 별도 필드 스텁 없이 존재하기만 하면 된다.
    private Chapter mockChapter() {
        return mock(Chapter.class);
    }

    private Lecture mockLecture(Long id, Long chapterId, String title, int orderNo) {
        Chapter chapter = mock(Chapter.class);
        given(chapter.getId()).willReturn(chapterId);

        Lecture lecture = mock(Lecture.class);
        given(lecture.getId()).willReturn(id);
        given(lecture.getTitle()).willReturn(title);
        given(lecture.getOrderNo()).willReturn(orderNo);
        given(lecture.getM3u8Path()).willReturn("/path/to/video.m3u8");
        given(lecture.getDurationSeconds()).willReturn(600);
        given(lecture.getChapter()).willReturn(chapter);
        return lecture;
    }

    private LectureProgress mockProgress(Long userId, Long lectureId, int lastPos, boolean completed) {
        LectureProgress progress = mock(LectureProgress.class);
        given(progress.getLastPositionSeconds()).willReturn(lastPos);
        given(progress.getWatchedSeconds()).willReturn(lastPos);
        given(progress.getProgressRate()).willReturn(50);
        given(progress.getCompleted()).willReturn(completed);
        return progress;
    }
}
