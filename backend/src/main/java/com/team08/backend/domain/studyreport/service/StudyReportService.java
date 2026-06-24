package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureqna.repository.QnaQuestionRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.exception.StudyNotFoundException;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyreport.dto.DailyProgressEntry;
import com.team08.backend.domain.studyreport.dto.ReportStatus;
import com.team08.backend.domain.studyreport.dto.StudyReportResponse;
import com.team08.backend.domain.studyreport.dto.TopLectureEntry;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.studyreport.util.StudyReportJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyReportService {

    /** 리포트 재집계 최소 간격. 마지막 갱신 후 이 시간 이내 요청은 기존 리포트를 그대로 반환한다. */
    private static final Duration REGENERATION_COOLDOWN = Duration.ofHours(1);

    private final StudyReportRepository studyReportRepository;
    private final StudyRepository studyRepository;
    private final LearningEventRepository learningEventRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final StudyReportJson studyReportJson;
    private final ObjectMapper objectMapper;

    /**
     * 리포트를 조회하거나, 필요 시 집계해 갱신하는 read-or-upsert 진입점.
     *
     * <ul>
     *   <li>refresh=false (조회 의도): 기존 리포트가 있으면 그대로 반환(LOADED).
     *       없으면 최초 집계 후 반환(REGENERATED).</li>
     *   <li>refresh=true (갱신 의도): 쿨다운이 지났거나 리포트가 없으면 재집계(REGENERATED).
     *       쿨다운 이내면 재집계를 건너뛰고 기존 리포트를 반환(COOLDOWN).</li>
     * </ul>
     */
    @Transactional
    public StudyReportResponse getReport(Long userId, Long studyId, boolean refresh) {
        StudyReport existing = studyReportRepository.findByUserIdAndStudyId(userId, studyId).orElse(null);

        if (existing != null) {
            // 조회 의도이거나, 갱신 의도지만 아직 쿨다운 이내라면 재집계 없이 기존 리포트를 반환한다.
            if (!refresh) {
                return response(existing, ReportStatus.LOADED);
            }
            if (isWithinCooldown(existing)) {
                return response(existing, ReportStatus.COOLDOWN);
            }
        }

        // 여기 도달: 리포트가 없거나(최초 집계), 갱신 의도이면서 쿨다운이 지난 경우.
        StudyReport report = regenerate(userId, studyId, existing);
        return response(report, ReportStatus.REGENERATED);
    }

    private StudyReport regenerate(Long userId, Long studyId, StudyReport existing) {
        // 레포트에 들어가는 내용: 총 시청시간 / 질문 개수 / 강좌 진행 률 / 학습한 일 수 / 최고 많이 학습한 강의 / 하루 학습량 맵 / 하루 학습량 맵
        Study study = studyRepository.findById(studyId)
                .orElseThrow(StudyNotFoundException::new);

        Long courseId = study.getCourse().getId();

        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        int totalLectureCount = lectureIds.size();

        // 총 시청시간: lecture_progresses 에서 강의당 1행을 단순 합산 (이벤트 중복 합산 없음)
        int totalWatchTime = lectureIds.isEmpty() ? 0
                : lectureProgressRepository.sumWatchedSecondsByUserIdAndLectureIdIn(userId, lectureIds);

        // 학습한 일 수: 날짜 이력이 필요하므로 learning_events 에서 집계
        int studyDays = learningEventRepository.countStudyDaysByUserIdAndCourseId(userId, courseId);

        long completedCount = totalLectureCount == 0 ? 0
                : lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true);
        BigDecimal progressRate = totalLectureCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount)
                        .divide(BigDecimal.valueOf(totalLectureCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP); // 강좌 진행 률 집계

        int totalQnaCount = lectureIds.isEmpty() ? 0                  //총 질문 수 집계
                : (int) qnaQuestionRepository.countByUserIdAndLectureIdIn(userId, lectureIds);

        String topLecturesJson = buildTopLecturesJson(userId, lectureIds);    //최고 많이 들었던 강의 집계
        String dailyProgressJson = buildDailyProgressJson(userId, courseId, totalLectureCount); //하루 학습량
        String dailyActivityMapJson = buildDailyActivityMapJson(userId, courseId);  //맵 제작

        StudyReport report = existing != null ? existing
                : studyReportRepository.save(StudyReport.create(userId, studyId));

        report.update(totalWatchTime, totalQnaCount, progressRate, studyDays,
                topLecturesJson, dailyProgressJson, dailyActivityMapJson);

        return report;
    }

    private boolean isWithinCooldown(StudyReport report) {
        return report.getUpdatedAt() != null
                && report.getUpdatedAt().isAfter(LocalDateTime.now().minus(REGENERATION_COOLDOWN));
    }

    /** 쿨다운이 해제되는(다음 재집계가 가능한) 시각. updatedAt 이 없으면 즉시 가능으로 본다. */
    private LocalDateTime nextRegenerableAt(StudyReport report) {
        return report.getUpdatedAt() == null ? null
                : report.getUpdatedAt().plus(REGENERATION_COOLDOWN);
    }

    private StudyReportResponse response(StudyReport report, ReportStatus status) {
        return StudyReportResponse.from(report, status, nextRegenerableAt(report), objectMapper);
    }

    // ── 집계 헬퍼 ─────────────────────────────────────────────────────────

    private String buildTopLecturesJson(Long userId, List<Long> lectureIds) {
        if (lectureIds.isEmpty()) return studyReportJson.write(List.of());

        List<LectureProgress> rows = lectureProgressRepository
                .findTop3ByUserIdAndLectureIdInOrderByWatchedSecondsDesc(userId, lectureIds);
        if (rows.isEmpty()) return studyReportJson.write(List.of());

        List<Long> topLectureIds = rows.stream()
                .map(LectureProgress::getLectureId)
                .toList();

        Map<Long, String> titleMap = lectureRepository.findIdAndTitleByIdIn(topLectureIds).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> (String) r[1]));

        List<TopLectureEntry> result = rows.stream()
                .map(p -> new TopLectureEntry(
                        p.getLectureId(),
                        titleMap.getOrDefault(p.getLectureId(), ""),
                        p.getWatchedSeconds()))
                .toList();

        return studyReportJson.write(result);
    }

    // TODO: 이 아래 함수들은 비싼함수이기 때문에 최적화가 시급
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
