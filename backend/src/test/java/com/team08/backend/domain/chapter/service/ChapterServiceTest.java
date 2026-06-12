package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
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

    // ── 챕터 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("챕터 생성 성공")
    void createChapter_success() {
        Long courseId = 1L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);
        Course course = TestEntityFactory.course(courseId);

        Chapter savedChapter = ChapterFixture.chapter(10L, request.title(), request.orderNo(), course);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(chapterRepository.save(any(Chapter.class))).willReturn(savedChapter);

        Long chapterId = chapterService.createChapter(courseId, request);

        assertThat(chapterId).isEqualTo(10L);
        verify(courseRepository).findById(courseId);
        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    @DisplayName("챕터 생성 실패 - 존재하지 않는 강좌로 챕터 생성 시")
    void createChapter_courseNotFound(){
        Long invalidCourseId = 999L;
        ChapterCreateRequest request = new ChapterCreateRequest("오리엔테이션", 1);

        given(courseRepository.findById(invalidCourseId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.createChapter(invalidCourseId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_NOT_FOUND.getMessage());

        verify(courseRepository).findById(invalidCourseId);
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
    @DisplayName("최근 수강 강의 조회 성공")
    void getLastWatchedLecture_success() {
        Long courseId = 1L;
        Long userId = 1L;
        Long lectureId = 100L;

        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(List.of(lectureId));

        LectureProgress progress = mockProgress(userId, lectureId, 300, false);
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

    // ── 챕터 첫 강의 입장 ──────────────────────────────────────────────────

    @Test
    @DisplayName("챕터 첫 강의 입장 성공 - 학습 이력 있음")
    void enterFirstLecture_success_withProgress() {
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        Chapter chapter = mock(Chapter.class);
        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

        Lecture lecture = mockLecture(lectureId, chapterId, "강의1", 1);
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(Optional.of(lecture));

        LectureProgress progress = mockProgress(userId, lectureId, 120, false);
        given(lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId))
                .willReturn(Optional.of(progress));

        LectureEnterResponse response = chapterService.enterFirstLecture(chapterId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.title()).isEqualTo("강의1");
        assertThat(response.progress()).isNotNull();
        assertThat(response.progress().lastPositionSeconds()).isEqualTo(120);
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 성공 - 학습 이력 없음 (progress null)")
    void enterFirstLecture_success_withoutProgress() {
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(mock(Chapter.class)));

        Lecture lecture = mockLecture(lectureId, chapterId, "강의1", 1);
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(Optional.of(lecture));

        given(lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId))
                .willReturn(Optional.empty());

        LectureEnterResponse response = chapterService.enterFirstLecture(chapterId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.progress()).isNull();
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 실패 - 챕터 없음")
    void enterFirstLecture_chapterNotFound() {
        given(chapterRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.enterFirstLecture(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }

    @Test
    @DisplayName("챕터 첫 강의 입장 실패 - 챕터에 강의 없음")
    void enterFirstLecture_lectureNotFound() {
        given(chapterRepository.findById(any())).willReturn(Optional.of(mock(Chapter.class)));
        given(lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.enterFirstLecture(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER);
    }


    // ── 헬퍼 ──────────────────────────────────────────────────────────────

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
        given(progress.getProgressRate()).willReturn(BigDecimal.valueOf(50.00));
        given(progress.getCompleted()).willReturn(completed);
        return progress;
    }
}
