package com.team08.backend.domain.report.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.repository.LectureCommentRepository;
import com.team08.backend.domain.lecture.repository.LectureProgressRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.report.dto.StudyReportResponse;
import com.team08.backend.domain.report.entity.DailyLectureStat;
import com.team08.backend.domain.report.entity.StudyReport;
import com.team08.backend.domain.report.repository.DailyLectureStatRepository;
import com.team08.backend.domain.report.repository.StudyReportRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberRole;
import com.team08.backend.domain.study.entity.StudyMemberStatus;
import com.team08.backend.domain.study.repository.StudyMemberRepository;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyReportService 단위 테스트")
class StudyReportServiceTest {

    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyMemberRepository studyMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private LectureCommentRepository lectureCommentRepository;
    @Mock
    private DailyLectureStatRepository dailyLectureStatRepository;
    @Mock
    private StudyReportRepository studyReportRepository;

    @InjectMocks
    private StudyReportService studyReportService;

    private User user;
    private Course course;
    private Study study;
    private StudyMember member;

    @BeforeEach
    void setUp() {
        user = TestEntityFactory.user("test@example.com", "테스트유저");
        ReflectionTestUtils.setField(user, "id", 1L);
        course = TestEntityFactory.course(user, "스프링 부트");
        ReflectionTestUtils.setField(course, "id", 10L);
        study = TestEntityFactory.study(
                course, user, "8기 스터디",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 5, 31)
        );
        ReflectionTestUtils.setField(study, "id", 100L);
        member = TestEntityFactory.studyMember(study, user, StudyMemberRole.MEMBER);
    }

    @Nested
    @DisplayName("리포트 발급")
    class IssueReport {

//        @Test
//        @DisplayName("스터디 기간이 완료되면 학습 리포트를 발급한다")
//        void issueReportSuccess() {
//            DailyLectureStat stat = DailyLectureStat.create(user, course, LocalDate.of(2026, 3, 1));
//            ReflectionTestUtils.setField(stat, "completedLectureCount", 2);
//
//            given(studyRepository.findById(100L)).willReturn(Optional.of(study));
//            given(userRepository.findById(1L)).willReturn(Optional.of(user));
//            given(studyMemberRepository.findByStudyIdAndUserId(100L, 1L)).willReturn(Optional.of(member));
//            given(lectureRepository.countByChapterCourseId(10L)).willReturn(10L);
//            given(lectureProgressRepository.countByUserIdAndLectureChapterCourseIdAndCompletedTrue(1L, 10L)).willReturn(5L);
//            given(lectureProgressRepository.sumWatchedSeconds(1L, 10L)).willReturn(3600);
//            given(lectureCommentRepository.countByUserIdAndLectureChapterCourseIdAndDeletedFalse(1L, 10L)).willReturn(7L);
//            given(dailyLectureStatRepository.findByUserIdAndCourseIdAndStatDateBetweenOrderByStatDateAsc(
//                    1L, 10L, study.getStartDate(), study.getEndDate()
//            )).willReturn(List.of(stat));
//            when(studyReportRepository.save(any(StudyReport.class))).thenAnswer(invocation -> {
//                StudyReport report = invocation.getArgument(0);
//                ReflectionTestUtils.setField(report, "totalWatchTime", 3600);
//                ReflectionTestUtils.setField(report, "progressRate", new BigDecimal("50.00"));
//                return report;
//            });
//
//            StudyReportResponse response = studyReportService.issueReport(1L, 100L);
//
//            assertThat(response.studyId()).isEqualTo(100L);
//            assertThat(response.totalWatchTimeSeconds()).isEqualTo(3600);
//            assertThat(response.totalComments()).isEqualTo(7L);
//            assertThat(response.completedLectures()).isEqualTo(5L);
//            assertThat(response.totalLectures()).isEqualTo(10L);
//            assertThat(response.progressRate()).isEqualByComparingTo("50.00");
//            assertThat(response.heatmap()).hasSize(1);
//            assertThat(response.heatmap().get(0).completedLectureCount()).isEqualTo(2);
//        }

        @Test
        @DisplayName("스터디 기간이 아직 완료되지 않으면 400을 반환한다")
        void studyNotEnded() {
            Study ongoingStudy = TestEntityFactory.study(
                    course, user, "진행 중 스터디",
                    LocalDate.now().minusDays(10),
                    LocalDate.now().plusDays(10)
            );
            ReflectionTestUtils.setField(ongoingStudy, "id", 101L);

            given(studyRepository.findById(101L)).willReturn(Optional.of(ongoingStudy));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(studyMemberRepository.findByStudyIdAndUserId(101L, 1L)).willReturn(Optional.of(member));

            assertThatThrownBy(() -> studyReportService.issueReport(1L, 101L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("스터디 구성원이 아니면 403을 반환한다")
        void notMember() {
            given(studyRepository.findById(100L)).willReturn(Optional.of(study));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(studyMemberRepository.findByStudyIdAndUserId(100L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studyReportService.issueReport(1L, 100L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("비활성 구성원은 리포트를 발급받을 수 없다")
        void inactiveMember() {
            ReflectionTestUtils.setField(member, "status", StudyMemberStatus.LEFT);

            given(studyRepository.findById(100L)).willReturn(Optional.of(study));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(studyMemberRepository.findByStudyIdAndUserId(100L, 1L)).willReturn(Optional.of(member));

            assertThatThrownBy(() -> studyReportService.issueReport(1L, 100L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
