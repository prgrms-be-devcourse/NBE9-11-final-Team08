package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.dto.LectureEnterResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChapterServiceTest {

    @Mock ChapterRepository chapterRepository;
    @Mock LectureRepository lectureRepository;
    @Mock LectureProgressRepository lectureProgressRepository;

    @InjectMocks ChapterService chapterService;

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

    // ── 최근 수강 강의 입장 ────────────────────────────────────────────────

    @Test
    @DisplayName("최근 수강 강의 입장 성공")
    void enterRecentLecture_success() {
        Long chapterId = 10L;
        Long userId = 1L;
        Long lectureId = 100L;

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(mock(Chapter.class)));

        Lecture lecture = mockLecture(lectureId, chapterId, "강의2", 2);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(List.of(lecture));

        LectureProgress progress = mockProgress(userId, lectureId, 300, true);
        given(lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(eq(userId), any()))
                .willReturn(Optional.of(progress));

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));

        LectureEnterResponse response = chapterService.enterRecentLecture(chapterId, userId);

        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.progress()).isNotNull();
        assertThat(response.progress().completed()).isTrue();
    }

    @Test
    @DisplayName("최근 수강 강의 입장 실패 - 학습 이력 없음")
    void enterRecentLecture_noHistory() {
        Long chapterId = 10L;

        given(chapterRepository.findById(chapterId)).willReturn(Optional.of(mock(Chapter.class)));

        Lecture lecture = mockLecture(100L, chapterId, "강의1", 1);
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(chapterId))
                .willReturn(List.of(lecture));

        given(lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.enterRecentLecture(chapterId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECENT_LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("최근 수강 강의 입장 실패 - 챕터에 강의 없음")
    void enterRecentLecture_noLectures() {
        given(chapterRepository.findById(any())).willReturn(Optional.of(mock(Chapter.class)));
        given(lectureRepository.findByChapterIdOrderByOrderNoAsc(any())).willReturn(List.of());

        assertThatThrownBy(() -> chapterService.enterRecentLecture(10L, 1L))
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
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getChapter()).willReturn(chapter);
        return lecture;
    }

    private LectureProgress mockProgress(Long userId, Long lectureId, int lastPos, boolean completed) {
        LectureProgress progress = mock(LectureProgress.class);
        given(progress.getLectureId()).willReturn(lectureId);
        given(progress.getUserId()).willReturn(userId);
        given(progress.getLastPositionSeconds()).willReturn(lastPos);
        given(progress.getWatchedSeconds()).willReturn(lastPos);
        given(progress.getProgressRate()).willReturn(BigDecimal.valueOf(50.00));
        given(progress.getCompleted()).willReturn(completed);
        given(progress.getUpdatedAt()).willReturn(LocalDateTime.now());
        return progress;
    }
}
