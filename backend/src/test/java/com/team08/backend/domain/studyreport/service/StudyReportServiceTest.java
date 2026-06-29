package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.access.LectureAccessValidator;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyreport.dto.ReportStatus;
import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyDailyStatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyReportServiceTest {

    @Mock StudyReportRepository studyReportRepository;
    @Mock StudyRepository studyRepository;
    @Mock StudyDailyStatRepository studyDailyStatRepository;
    @Mock LectureProgressRepository lectureProgressRepository;
    @Mock LectureRepository lectureRepository;
    @Mock QnaQuestionRepository qnaQuestionRepository;
    @Mock LectureAccessValidator lectureAccessValidator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final StudyReportJson studyReportJson = new StudyReportJson(objectMapper);

    @InjectMocks StudyReportService studyReportService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(studyReportService, "studyReportJson", studyReportJson);
        org.springframework.test.util.ReflectionTestUtils.setField(studyReportService, "objectMapper", objectMapper);
    }

    // ── getReport: 갱신(REGENERATED) ───────────────────────────────

    @Test
    @DisplayName("리포트가 없으면 조회(refresh=false) 시에도 새로 집계해 REGENERATED로 반환한다")
    void getOrRefresh_missing_regenerates() {
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

        StudyReportResponse response = studyReportService.getReport(userId, studyId, false);

        assertThat(response.status()).isEqualTo(ReportStatus.REGENERATED);
        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.totalWatchTime()).isEqualTo(3600);
        assertThat(response.totalQnaCount()).isEqualTo(5);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
        verify(studyReportRepository).save(any());
    }

    @Test
    @DisplayName("기존 리포트가 있고 쿨다운이 지났으면 refresh=true 시 재집계해 덮어쓴다")
    void getOrRefresh_refreshAfterCooldown_overwrites() {
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
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.now().minusHours(2)); // 쿨다운 경과
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(existing));

        StudyReportResponse response = studyReportService.getReport(userId, studyId, true);

        assertThat(response.status()).isEqualTo(ReportStatus.REGENERATED);
        assertThat(response.totalWatchTime()).isEqualTo(7200);
        assertThat(response.totalQnaCount()).isEqualTo(3);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        verify(studyReportRepository, never()).save(any());
    }

    // ── getReport: 갱신 불가(COOLDOWN) ─────────────────────────────

    @Test
    @DisplayName("refresh=true 라도 쿨다운 이내면 재집계 없이 기존 리포트를 COOLDOWN으로 반환한다")
    void getOrRefresh_refreshWithinCooldown_returnsCooldown() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;

        stubStudy(studyId, courseId); // 접근 권한 검증 통과
        StudyReport recent = StudyReport.create(userId, studyId);
        recent.update(1234, 5, 0, 0, 3, "[]", "[]", "{}");
        ReflectionTestUtils.setField(recent, "id", 7L);
        LocalDateTime updatedAt = LocalDateTime.now().minusMinutes(10);
        ReflectionTestUtils.setField(recent, "updatedAt", updatedAt);
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(recent));

        StudyReportResponse response = studyReportService.getReport(userId, studyId, true);

        assertThat(response.status()).isEqualTo(ReportStatus.COOLDOWN);
        assertThat(response.totalWatchTime()).isEqualTo(1234);
        assertThat(response.nextRegenerableAt()).isEqualTo(updatedAt.plusHours(1));
        // 쿨다운: 비싼 집계 경로(강의 조회·저장)를 타지 않아야 한다
        verify(lectureRepository, never()).findIdsByCourseId(any());
        verify(studyReportRepository, never()).save(any());
    }

    // ── getReport: 조회(LOADED) ────────────────────────────────────

    @Test
    @DisplayName("기존 리포트가 있으면 refresh=false 시 재집계 없이 LOADED로 조회한다")
    void getOrRefresh_loadExisting_returnsLoaded() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;

        stubStudy(studyId, courseId); // 접근 권한 검증 통과
        StudyReport report = StudyReport.create(userId, studyId);
        // progressRate(75.00)는 completed/total(3/4)에서 파생된다.
        report.update(5000, 4, 3, 4, 10, "[]", "[]", "{}");
        ReflectionTestUtils.setField(report, "id", 1L);
        ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.now().minusDays(3)); // 쿨다운 지났어도 조회만
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(report));

        StudyReportResponse response = studyReportService.getReport(userId, studyId, false);

        assertThat(response.status()).isEqualTo(ReportStatus.LOADED);
        assertThat(response.studyId()).isEqualTo(studyId);
        assertThat(response.totalWatchTime()).isEqualTo(5000);
        assertThat(response.totalQnaCount()).isEqualTo(4);
        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        assertThat(response.studyDays()).isEqualTo(10);
        // 접근 권한 검증은 거치되, 조회는 집계 경로를 타지 않는다
        verify(lectureAccessValidator).validateCourseAccess(courseId, userId);
        verify(lectureRepository, never()).findIdsByCourseId(any());
        verify(studyReportRepository, never()).save(any());
    }

    // ── getReport: 엣지 ────────────────────────────────────────────

    @Test
    @DisplayName("강좌에 강의가 없으면 progressRate는 0이다")
    void getOrRefresh_noLectures_progressRateZero() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;

        stubCommon(userId, studyId, courseId, List.of());
        given(studyReportRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.empty());

        StudyReport saved = StudyReport.create(userId, studyId);
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(studyReportRepository.save(any())).willReturn(saved);

        StudyReportResponse response = studyReportService.getReport(userId, studyId, false);

        assertThat(response.progressRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("스터디가 없으면 예외가 발생한다")
    void getOrRefresh_studyNotFound_throwsException() {
        given(studyRepository.findByIdWithCourse(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studyReportService.getReport(1L, 99L, false))
                .isInstanceOf(StudyNotFoundException.class);
    }

    @Test
    @DisplayName("스터디 상태와 무관하게, 강좌 접근 권한이 없으면 리포트를 조회할 수 없다")
    void getReport_courseAccessDenied_throwsBeforeReportLookup() {
        Long userId = 1L;
        Long studyId = 10L;
        Long courseId = 100L;

        stubStudy(studyId, courseId);
        willThrow(new CustomException(ErrorCode.COURSE_ACCESS_DENIED))
                .given(lectureAccessValidator).validateCourseAccess(courseId, userId);

        assertThatThrownBy(() -> studyReportService.getReport(userId, studyId, false))
                .isInstanceOf(CustomException.class);

        // 권한 검증이 리포트 조회보다 먼저 일어나, 권한 없으면 리포트에 접근조차 하지 않는다.
        verify(studyReportRepository, never()).findByUserIdAndStudyId(any(), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void stubCommon(Long userId, Long studyId, Long courseId, List<Long> lectureIds) {
        stubStudy(studyId, courseId);
        given(lectureRepository.findIdsByCourseId(courseId)).willReturn(lectureIds);

        given(lectureProgressRepository.findTop3ByUserIdAndLectureIdInOrderByWatchedSecondsDesc(userId, lectureIds))
                .willReturn(List.of());
        // 일별 진도/활동/학습일수는 롤업에서 읽는다.
        given(studyDailyStatRepository.findByUserIdAndCourseIdOrderByActivityDateAsc(userId, courseId))
                .willReturn(List.of());
        given(lectureRepository.findIdAndTitleByIdIn(any())).willReturn(List.of());
    }

    /** getReport 진입 시 항상 수행되는 "스터디→강좌 조회 + 강좌 접근 권한 검증"을 통과하도록 스텁한다. */
    private void stubStudy(Long studyId, Long courseId) {
        Study study = mockStudy(studyId, courseId); // 중첩 스터빙 방지: given() 밖에서 먼저 생성
        given(studyRepository.findByIdWithCourse(studyId)).willReturn(Optional.of(study));
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
