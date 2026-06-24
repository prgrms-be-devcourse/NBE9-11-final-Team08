package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
import com.team08.backend.domain.studyreport.exception.StudyReportNotFoundException;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.studyreport.util.StudyReportJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyReportServiceTest {

    @Mock StudyReportRepository studyReportRepository;
    @Mock StudyRepository studyRepository;
    @Mock LearningEventRepository learningEventRepository;
    @Mock LectureProgressRepository lectureProgressRepository;
    @Mock LectureRepository lectureRepository;
    @Mock QnaQuestionRepository qnaQuestionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final StudyReportJson studyReportJson = new StudyReportJson(objectMapper);

    @InjectMocks StudyReportService studyReportService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(studyReportService, "studyReportJson", studyReportJson);
        org.springframework.test.util.ReflectionTestUtils.setField(studyReportService, "objectMapper", objectMapper);
    }

    // ── generateReport ────────────────────────────────────────────────────

    @Test
    @DisplayName("리포트가 없을 때 새로 생성된다")
    void generateReport_create_success() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;
        List<Long> lectureIds = List.of(1L, 2L, 3L);

        stubCommon(userId, studyId, courseId, lectureIds);
        given(lectureProgressRepository.sumWatchedSecondsByUserIdAndLectureIdIn(userId, lectureIds)).willReturn(3600);
        given(lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true)).willReturn(2L);
        given(qnaQuestionRepository.countByUserIdAndLectureIdIn(userId, lectureIds)).willReturn(5L);
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.empty());

        StudyReport saved = StudyReport.create(userId, studyId);
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(studyReportRepository.save(any())).willReturn(saved);

        StudyReportResponse response = studyReportService.generateReport(userId, studyId);

        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.totalWatchTime()).isEqualTo(3600);
        assertThat(response.totalQnaCount()).isEqualTo(5);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
        verify(studyReportRepository).save(any());
    }

    @Test
    @DisplayName("기존 리포트가 있을 때 덮어쓴다")
    void generateReport_overwrite_success() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;
        List<Long> lectureIds = List.of(1L, 2L);

        stubCommon(userId, studyId, courseId, lectureIds);
        given(lectureProgressRepository.sumWatchedSecondsByUserIdAndLectureIdIn(userId, lectureIds)).willReturn(7200);
        given(lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true)).willReturn(2L);
        given(qnaQuestionRepository.countByUserIdAndLectureIdIn(userId, lectureIds)).willReturn(3L);

        StudyReport existing = StudyReport.create(userId, studyId);
        ReflectionTestUtils.setField(existing, "id", 5L);
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(existing));

        StudyReportResponse response = studyReportService.generateReport(userId, studyId);

        assertThat(response.totalWatchTime()).isEqualTo(7200);
        assertThat(response.totalQnaCount()).isEqualTo(3);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        verify(studyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("마지막 갱신 후 1시간 이내면 재집계 없이 기존 리포트를 반환한다")
    void generateReport_withinCooldown_returnsExistingWithoutRecompute() {
        Long userId = 1L;
        Long studyId = 10L;

        StudyReport recent = StudyReport.create(userId, studyId);
        recent.update(1234, 5, BigDecimal.valueOf(50.00), 3, "[]", "[]", "{}");
        ReflectionTestUtils.setField(recent, "id", 7L);
        ReflectionTestUtils.setField(recent, "updatedAt", LocalDateTime.now().minusMinutes(10));
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(recent));

        StudyReportResponse response = studyReportService.generateReport(userId, studyId);

        assertThat(response.totalWatchTime()).isEqualTo(1234);
        // 쿨다운: 비싼 집계 경로(스터디/강의 조회·저장)를 타지 않아야 한다
        verify(studyRepository, never()).findById(any());
        verify(studyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("강좌에 강의가 없으면 progressRate는 0이다")
    void generateReport_noLectures_progressRateZero() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;

        stubCommon(userId, studyId, courseId, List.of());
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.empty());

        StudyReport saved = StudyReport.create(userId, studyId);
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(studyReportRepository.save(any())).willReturn(saved);

        StudyReportResponse response = studyReportService.generateReport(userId, studyId);

        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("스터디가 없으면 예외가 발생한다")
    void generateReport_studyNotFound_throwsException() {
        given(studyRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyReportService.generateReport(1L, 99L))
                .isInstanceOf(StudyNotFoundException.class);
    }

    // ── getReport ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("저장된 리포트를 정상 조회한다")
    void getReport_success() {
        Long userId = 1L;
        Long studyId = 10L;

        StudyReport report = StudyReport.create(userId, studyId);
        report.update(5000, 4, BigDecimal.valueOf(75.00), 10, "[]", "[]", "{}");
        ReflectionTestUtils.setField(report, "id", 1L);
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(report));

        StudyReportResponse response = studyReportService.getReport(userId, studyId);

        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.totalWatchTime()).isEqualTo(5000);
        assertThat(response.totalQnaCount()).isEqualTo(4);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        assertThat(response.studyDays()).isEqualTo(10);
    }

    @Test
    @DisplayName("리포트가 없으면 예외가 발생한다")
    void getReport_notFound_throwsException() {
        given(studyReportRepository.findByUserIdAndStudyId(1L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyReportService.getReport(1L, 99L))
                .isInstanceOf(StudyReportNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void stubCommon(Long userId, Long studyId, Long courseId, List<Long> lectureIds) {
        Study study = mockStudy(studyId, courseId);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(lectureIds);

        given(learningEventRepository.countStudyDaysByUserIdAndCourseId(userId, courseId)).willReturn(5);

        given(lectureProgressRepository.findTop3ByUserIdAndLectureIdInOrderByWatchedSecondsDesc(userId, lectureIds))
                .willReturn(List.of());
        given(learningEventRepository.findDailyCompletionCounts(userId, courseId)).willReturn(List.of());
        given(learningEventRepository.findDailyActivityCounts(userId, courseId)).willReturn(List.of());
        given(lectureRepository.findIdAndTitleByIdIn(any())).willReturn(List.of());
    }

    private Study mockStudy(Long studyId, Long courseId) {
        Course course = mock(Course.class);
        given(course.getId()).willReturn(courseId);

        Study study = mock(Study.class);
        given(study.getId()).willReturn(studyId);
        given(study.getCourse()).willReturn(course);
        return study;
    }
}
