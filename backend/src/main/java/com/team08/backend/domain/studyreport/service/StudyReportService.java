package com.team08.backend.domain.studyreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team08.backend.domain.lecture.access.LectureAccessValidator;
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
import com.team08.backend.domain.studyreport.entity.StudyDailyStat;
import com.team08.backend.domain.studyreport.entity.StudyReport;
import com.team08.backend.domain.studyreport.repository.StudyDailyStatRepository;
import com.team08.backend.domain.studyreport.repository.StudyReportRepository;
import com.team08.backend.domain.studyreport.util.StudyReportJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
    private final StudyDailyStatRepository studyDailyStatRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final LectureAccessValidator lectureAccessValidator;
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
        // 스터디의 상태(모집중/활성/종료 등)와 무관하게, 해당 강좌에 접근할 수 있으면
        // (수강 중이거나 강좌 소유자) 리포트를 볼 수 있다. 스터디 권한이 아니라 강좌 접근 권한으로
        // 판단하도록 LectureAccessValidator(강좌 단위 검증)에 위임한다.
        Study study = studyRepository.findByIdWithCourse(studyId)
                .orElseThrow(StudyNotFoundException::new);
        lectureAccessValidator.validateCourseAccess(study.getCourse().getId(), userId);

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
        StudyReport report = regenerate(userId, study, existing);
        return response(report, ReportStatus.REGENERATED);
    }

    private StudyReport regenerate(Long userId, Study study, StudyReport existing) {
        // 레포트에 들어가는 내용: 총 시청시간 / 질문 개수 / 강좌 진행 률 / 학습한 일 수 / 최고 많이 학습한 강의 / 하루 학습량 맵 / 하루 학습량 맵
        Long studyId = study.getId();
        Long courseId = study.getCourse().getId();

        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        int totalLectureCount = lectureIds.size();

        // 총 시청시간: lecture_progresses 에서 강의당 1행을 단순 합산 (이벤트 중복 합산 없음)
        int totalWatchTime = lectureIds.isEmpty() ? 0
                : lectureProgressRepository.sumWatchedSecondsByUserIdAndLectureIdIn(userId, lectureIds);

        long completedCount = totalLectureCount == 0 ? 0
                : lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true);

        int totalQnaCount = lectureIds.isEmpty() ? 0                  //총 질문 수 집계
                : (int) qnaQuestionRepository.countByUserIdAndLectureIdIn(userId, lectureIds);

        String topLecturesJson = buildTopLecturesJson(userId, lectureIds);    //최고 많이 들었던 강의 집계

        // 학습일수·일별진도·일별활동맵은 learning_events 스캔/GROUP BY 대신
        // 사전 집계된 일별 롤업(learning_daily_stats)을 한 번 읽어 만든다.
        List<StudyDailyStat> dailyStats = studyDailyStatRepository
                .findByUserIdAndCourseIdOrderByActivityDateAsc(userId, courseId);
        int studyDays = dailyStats.size(); // 행 1개 = 활동한 날짜 1일
        String dailyProgressJson = buildDailyProgressJson(dailyStats, totalLectureCount);
        String dailyActivityMapJson = buildDailyActivityMapJson(dailyStats);

        StudyReport report = existing != null ? existing
                : studyReportRepository.save(StudyReport.create(userId, studyId));

        report.update(totalWatchTime, totalQnaCount,
                (int) completedCount, totalLectureCount, studyDays,
                topLecturesJson, dailyProgressJson, dailyActivityMapJson);

        return report;
    }

    /**
     * 강의 완료 이벤트 시 해당 유저·스터디 리포트를 재집계한다.
     * 완료는 강의당 1회뿐인 희귀 이벤트라, 쿨다운 없이 매번 재집계해도 비용이 제한적이다.
     * (하트비트 같은 고빈도 경로에서는 호출하지 않는다.)
     */
    @Transactional
    public void refreshOnLectureCompletion(Long userId, Long courseId) {
        Study study = studyRepository.findByCourseIdWithCourse(courseId).orElse(null);
        if (study == null) {
            return; // 스터디가 없는 강좌면 집계할 리포트도 없다.
        }
        StudyReport existing = studyReportRepository
                .findByUserIdAndStudyId(userId, study.getId())
                .orElse(null);
        regenerate(userId, study, existing);
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

    /** 일별 롤업의 누적 완료수 / 총 강의수 × 100 → 날짜별 진도 시계열. (롤업은 날짜 오름차순 전제) */
    private String buildDailyProgressJson(List<StudyDailyStat> dailyStats, int totalLectureCount) {
        if (totalLectureCount == 0) return studyReportJson.write(List.of());

        List<DailyProgressEntry> entries = new ArrayList<>();
        long accumulated = 0;
        for (StudyDailyStat stat : dailyStats) {
            accumulated += stat.getCompletedCount();
            BigDecimal rate = BigDecimal.valueOf(accumulated)
                    .divide(BigDecimal.valueOf(totalLectureCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            entries.add(new DailyProgressEntry(stat.getActivityDate(), rate));
        }
        return studyReportJson.write(entries);
    }

    /** 일별 롤업의 날짜별 이벤트 수 → 캘린더 잔디용 맵. */
    private String buildDailyActivityMapJson(List<StudyDailyStat> dailyStats) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (StudyDailyStat stat : dailyStats) {
            map.put(stat.getActivityDate().toString(), stat.getEventCount());
        }
        return studyReportJson.write(map);
    }
}
