package com.team08.backend.domain.learningevent.event;

import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;

import java.time.LocalDateTime;

/**
 * 학습 이벤트가 적재된 직후 발행되는 단일 도메인 이벤트.
 * <p>
 * producer({@code LearningEventService})는 "무슨 일이 일어났다"만 알린다.
 * "그래서 무엇을 할지"(progress flush, 알림, 통계 등)는 각 리스너가 자기 타입만 필터링해 소유한다.
 * → 새 반응을 추가할 때 리스너 클래스만 늘리면 되고 producer 는 수정하지 않는다(개방-폐쇄).
 * <p>
 * 영속 엔티티 대신 값만 담는다(AFTER_COMMIT 리스너에서 detach/lazy 문제를 피하기 위함).
 */
public record LearningEventRecorded(
        Long learningEventId,
        Long userId,
        Long courseId,
        Long chapterId,
        Long lectureId,
        LearningEventType eventType,
        Integer positionSeconds,
        LocalDateTime eventTime
) {
    public static LearningEventRecorded from(LearningEvent e) {
        return new LearningEventRecorded(
                e.getId(),
                e.getUserId(),
                e.getCourseId(),
                e.getChapterId(),
                e.getLectureId(),
                e.getEventType(),
                e.getPositionSeconds(),
                e.getEventTime()
        );
    }
}
