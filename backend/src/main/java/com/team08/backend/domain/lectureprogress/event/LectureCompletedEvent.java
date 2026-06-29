package com.team08.backend.domain.lectureprogress.event;

import java.time.LocalDateTime;

/**
 * 한 강의의 진행률이 완료 임계치에 처음 도달해 {@code completed} 가 false→true 로 전이된 순간 발행되는 이벤트.
 * <p>
 * 완료는 강의당 1회뿐인 희귀 전이라, 이 이벤트를 받아 스터디 리포트를 재집계해도 비용이 제한적이다.
 * 하트비트(고빈도)에서는 발행되지 않으며, 진행률을 실제로 움직이는 완료 시점에만 발행된다.
 */
public record LectureCompletedEvent(
        Long userId,
        Long courseId,
        Long chapterId,
        Long lectureId,
        LocalDateTime eventTime
) {
}
