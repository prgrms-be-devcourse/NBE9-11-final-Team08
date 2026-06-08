package com.team08.backend.domain.report.service;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.repository.LectureCommentRepository;
import com.team08.backend.domain.lecture.repository.LectureProgressRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.report.dto.DailyLectureHeatmapResponse;
import com.team08.backend.domain.report.dto.StudyReportResponse;
import com.team08.backend.domain.report.entity.DailyLectureStat;
import com.team08.backend.domain.report.entity.StudyReport;
import com.team08.backend.domain.report.repository.DailyLectureStatRepository;
import com.team08.backend.domain.report.repository.StudyReportRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyMember;
import com.team08.backend.domain.study.entity.StudyMemberStatus;
import com.team08.backend.domain.study.repository.StudyMemberRepository;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyReportService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureCommentRepository lectureCommentRepository;
    private final DailyLectureStatRepository dailyLectureStatRepository;
    private final StudyReportRepository studyReportRepository;

    @Transactional
    public StudyReportResponse issueReport(Long userId, Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        StudyMember member = studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "스터디 구성원이 아닙니다."));

        if (member.getStatus() != StudyMemberStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 소속된 스터디 구성원이 아닙니다.");
        }
        if (study.getEndDate() == null || study.getEndDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스터디 기간이 아직 완료되지 않았습니다.");
        }

        Course course = study.getCourse();
        long totalLectures = lectureRepository.countByChapterCourseId(course.getId());
        long completedLectures = lectureProgressRepository.countByUserIdAndLectureChapterCourseIdAndCompletedTrue(userId, course.getId());
        Integer totalWatchTime = lectureProgressRepository.sumWatchedSeconds(userId, course.getId());
        long totalComments = lectureCommentRepository.countByUserIdAndLectureChapterCourseIdAndDeletedFalse(userId, course.getId());
        BigDecimal progressRate = calculateProgressRate(completedLectures, totalLectures);

        StudyReport savedReport = studyReportRepository.save(StudyReport.generate(
                user,
                course,
                totalWatchTime,
                Math.toIntExact(totalComments),
                progressRate
        ));

        List<DailyLectureHeatmapResponse> heatmap = getHeatmap(userId, course.getId(), study.getStartDate(), study.getEndDate());

        return new StudyReportResponse(
                studyId,
                userId,
                course.getId(),
                savedReport.getTotalWatchTime(),
                totalComments,
                completedLectures,
                totalLectures,
                savedReport.getProgressRate(),
                heatmap,
                savedReport.getGeneratedAt()
        );
    }

    private BigDecimal calculateProgressRate(long completedLectures, long totalLectures) {
        if (totalLectures == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(completedLectures)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLectures), 2, RoundingMode.HALF_UP);
    }

    private List<DailyLectureHeatmapResponse> getHeatmap(Long userId, Long courseId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? endDate.minusMonths(1) : startDate;
        List<DailyLectureStat> stats = dailyLectureStatRepository
                .findByUserIdAndCourseIdAndStatDateBetweenOrderByStatDateAsc(userId, courseId, start, endDate);
        return stats.stream()
                .map(DailyLectureHeatmapResponse::from)
                .toList();
    }
}
