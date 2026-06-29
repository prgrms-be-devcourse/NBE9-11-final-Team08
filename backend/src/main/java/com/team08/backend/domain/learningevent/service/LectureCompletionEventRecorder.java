package com.team08.backend.domain.learningevent.service;

import com.team08.backend.domain.lectureprogress.event.LectureCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 완료 전이(하트비트로 progressRate≥90% 도달)를 받아 정본 LECTURE_COMPLETE 학습이벤트를 적재한다.
 * <p>
 * 완강 판별의 단일 진실원천은 lecture_progresses(하트비트)다. 이 리스너가 완료를 학습이벤트로
 * 번역해주면, 그 뒤의 연쇄(LearningEventRecorded → 피드 / 일별 통계)와 대시보드 집계가 모두
 * 동일한 완료 정의를 공유한다. 클라이언트는 완료 이벤트를 직접 보내지 않는다.
 * <p>
 * AFTER_COMMIT: 진행률(완료) 트랜잭션이 커밋된 뒤에만 동작한다.
 * REQUIRES_NEW + best-effort: 적재 실패가 완료 상태를 되돌리지 않으며, 다음 완료/재처리로 복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LectureCompletionEventRecorder {

    private final LearningEventService learningEventService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLectureCompleted(LectureCompletedEvent event) {
        try {
            learningEventService.recordLectureCompletion(
                    event.userId(),
                    event.courseId(),
                    event.chapterId(),
                    event.lectureId(),
                    event.eventTime()
            );
        } catch (Exception e) {
            log.warn("완료 학습이벤트 적재 실패 - userId={}, lectureId={}",
                    event.userId(), event.lectureId(), e);
        }
    }
}
