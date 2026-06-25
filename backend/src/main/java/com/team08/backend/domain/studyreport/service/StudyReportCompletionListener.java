package com.team08.backend.domain.studyreport.service;

import com.team08.backend.domain.lectureprogress.event.LectureCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 강의 완료 이벤트를 받아 해당 유저·스터디의 학습 리포트를 증분 갱신한다.
 * <p>
 * AFTER_COMMIT: 완료(진행률) 트랜잭션이 커밋된 뒤에만 동작한다.
 * REQUIRES_NEW: 재집계는 새 트랜잭션에서 수행하고, 실패해도 swallow(로그만) 한다.
 * → 리포트 갱신이 실패해도 이미 커밋된 lecture_progresses 완료 상태는 영향받지 않으며,
 *   다음 완료 이벤트나 리포트 페이지의 재집계(refresh)가 자가복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyReportCompletionListener {

    private final StudyReportService studyReportService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLectureCompleted(LectureCompletedEvent event) {
        try {
            studyReportService.refreshOnLectureCompletion(event.userId(), event.courseId());
        } catch (Exception e) {
            // best-effort: 리포트 증분 갱신 실패는 학습 흐름을 막지 않는다(다음 완료/refresh 로 복구).
            log.warn("강의 완료 리포트 갱신 실패 - userId={}, courseId={}",
                    event.userId(), event.courseId(), e);
        }
    }
}
