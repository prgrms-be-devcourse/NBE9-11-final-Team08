package com.team08.backend.domain.learningevent.event;

import java.time.LocalDateTime;

/**
 * 강의 퇴장(LECTURE_EXIT) 이벤트가 적재된 뒤 발행되는 도메인 이벤트.
 * <p>
 * 퇴장의 1급 데이터는 이벤트 기록(단일 출처·비가역)이고,
 * 이 이벤트를 받아 수행하는 lecture_progresses 마지막 위치 flush 는
 * 하트비트로 이미 다중화된 best-effort 보정이다. 그래서 이벤트 적재가
 * 커밋된 뒤(AFTER_COMMIT)에만, 이벤트를 롤백시키지 않는 별도 트랜잭션에서 처리한다.
 */
public record LectureExitedEvent(
        Long userId,
        Long lectureId,
        Integer positionSeconds,
        LocalDateTime eventTime
) {
}
