package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.studyreport.repository.StudyDailyStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 학습 이벤트 적재에 대한 반응 중 하나: (사용자, 강좌, 날짜) 일별 롤업을 증분한다.
 * <p>
 * 단일 도메인 이벤트 {@link LearningEventRecorded} 를 구독해, 이벤트마다 {@code event_count += 1}
 * (완료 이벤트면 {@code completed_count += 1})를 원자적 UPSERT 한다. 리포트의 학습일수·일별진도·
 * 일별활동맵이 이 롤업에서 읽혀 learning_events GROUP BY 스캔이 사라진다.
 * <p>
 * AFTER_COMMIT + REQUIRES_NEW + best-effort: 이벤트 적재가 1급이고 롤업은 파생이라,
 * 갱신 실패 시 swallow(로그). 누락분은 learning_events 로부터의 재빌드(마이그레이션 백필과 동일 집계)로 복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsRollupListener {

    private final StudyDailyStatRepository studyDailyStatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLearningEventRecorded(LearningEventRecorded event) {
        // 강좌에 귀속되지 않는 이벤트는 일별 롤업 대상에서 제외(기존 learning_events 집계도 course_id 기준).
        if (event.courseId() == null || event.eventTime() == null) {
            return;
        }
        int completedDelta = event.eventType() == LearningEventType.LECTURE_COMPLETE ? 1 : 0;
        try {
            studyDailyStatRepository.upsertIncrement(
                    event.userId(),
                    event.courseId(),
                    event.eventTime().toLocalDate(),
                    completedDelta
            );
        } catch (Exception e) {
            log.warn("일별 롤업 갱신 실패 - userId={}, courseId={}, date={}",
                    event.userId(), event.courseId(), event.eventTime(), e);
        }
    }
}
