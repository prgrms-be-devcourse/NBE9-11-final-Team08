package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyreport.dto.DailyProgressEntry;
import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.dto.TopLectureEntry;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.exception.StudyReportNotFoundException;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.studyreport.util.StudyReportJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyReportService {

    private final StudyReportRepository studyReportRepository;
    private final StudyRepository studyRepository;
    private final LearningEventRepository learningEventRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final StudyReportJson studyReportJson;
    private final ObjectMapper objectMapper;

    @Transactional
    public StudyReportResponse generateReport(Long userId, Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(StudyNotFoundException::new);

        Long courseId = study.getCourse().getId();
        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        int totalLectureCount = lectureIds.size();

        Integer totalWatchTime = learningEventRepository.sumWatchTimeByUserIdAndCourseId(userId, courseId);
        Integer studyDays = learningEventRepository.countStudyDaysByUserIdAndCourseId(userId, courseId);

        long completedCount = totalLectureCount == 0 ? 0
                : lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true);
        BigDecimal progressRate = totalLectureCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount)
                        .divide(BigDecimal.valueOf(totalLectureCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        int totalQnaCount = lectureIds.isEmpty() ? 0
                : (int) qnaQuestionRepository.countByUserIdAndLectureIdIn(userId, lectureIds);

        String topLecturesJson = buildTopLecturesJson(userId, courseId);
        String dailyProgressJson = buildDailyProgressJson(userId, courseId, totalLectureCount);
        String dailyActivityMapJson = buildDailyActivityMapJson(userId, courseId);

        StudyReport report = studyReportRepository.findByUserIdAndStudyId(userId, studyId)
                .orElseGet(() -> studyReportRepository.save(StudyReport.create(userId, studyId)));

        report.update(totalWatchTime, totalQnaCount, progressRate, studyDays,
                topLecturesJson, dailyProgressJson, dailyActivityMapJson);

        return StudyReportResponse.from(report, objectMapper);
    }

    @Transactional(readOnly = true)
    public StudyReportResponse getReport(Long userId, Long studyId) {
        StudyReport report = studyReportRepository.findByUserIdAndStudyId(userId, studyId)
                .orElseThrow(StudyReportNotFoundException::new);
        return StudyReportResponse.from(report, objectMapper);
    }

    // ── 집계 헬퍼 ─────────────────────────────────────────────────────────

    private String buildTopLecturesJson(Long userId, Long courseId) {
        List<Object[]> rows = learningEventRepository.findTopLecturesByWatchTime(userId, courseId);
        if (rows.isEmpty()) return studyReportJson.write(List.of());

        List<Long> lectureIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        Map<Long, String> titleMap = lectureRepository.findIdAndTitleByIdIn(lectureIds).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> (String) r[1]));

        List<TopLectureEntry> result = rows.stream()
                .map(r -> new TopLectureEntry(
                        ((Number) r[0]).longValue(),
                        titleMap.getOrDefault(((Number) r[0]).longValue(), ""),
                        ((Number) r[1]).intValue()))
                .toList();

        return studyReportJson.write(result);
    }

    private String buildDailyProgressJson(Long userId, Long courseId, int totalLectureCount) {
        if (totalLectureCount == 0) return studyReportJson.write(List.of());

        List<Object[]> rows = learningEventRepository.findDailyCompletionCounts(userId, courseId);
        List<DailyProgressEntry> entries = new ArrayList<>();
        long accumulated = 0;

        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            accumulated += ((Number) row[1]).longValue();
            BigDecimal rate = BigDecimal.valueOf(accumulated)
                    .divide(BigDecimal.valueOf(totalLectureCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            entries.add(new DailyProgressEntry(date, rate));
        }

        return studyReportJson.write(entries);
    }

    private String buildDailyActivityMapJson(Long userId, Long courseId) {
        List<Object[]> rows = learningEventRepository.findDailyActivityCounts(userId, courseId);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        return studyReportJson.write(map);
    }
}
