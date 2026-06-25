package com.team08.backend.domain.lectureprogress.service;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 학습 이벤트 적재에 대한 반응 중 하나: 강의 퇴장(LECTURE_EXIT) 시 lecture_progresses 의
 * 마지막 시청 위치를 flush 한다.
 * <p>
 * 단일 도메인 이벤트 {@link LearningEventRecorded} 를 구독하고 자기 타입(LECTURE_EXIT)만 필터링한다.
 * AFTER_COMMIT: 퇴장 이벤트가 영속화된 뒤에만 동작한다.
 * REQUIRES_NEW: flush 는 새 트랜잭션에서 수행하고, 실패해도 swallow(로그만) 한다.
 * → flush 가 실패해도 이미 커밋된 LECTURE_EXIT 이벤트는 영향받지 않으며,
 *   위치 정밀도 손실은 다음 입장/하트비트가 자가복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExitProgressFlushListener {

    private final LectureProgressService lectureProgressService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLearningEventRecorded(LearningEventRecorded event) {
        if (event.eventType() != LearningEventType.LECTURE_EXIT) {
            return; // 이 리스너는 퇴장에만 반응한다.
        }
        if (event.positionSeconds() == null) {
            return;
        }
        try {
            lectureProgressService.upsertLastPosition(
                    event.userId(),
                    event.lectureId(),
                    event.positionSeconds(),
                    event.eventTime()
            );
        } catch (Exception e) {
            // best-effort: 진행상황 flush 실패는 퇴장 기록을 막지 않는다(하트비트로 복구).
            log.warn("강의 퇴장 progress flush 실패 - userId={}, lectureId={}",
                    event.userId(), event.lectureId(), e);
        }
    }
}
