-- 영상 시청 이벤트 모델 개편.
-- 학습 도메인에선 "많이 본 구간"보다 학습자가 "어려워서 멈춘 구간"이 더 중요하다.
-- 시작/재개 신호(VIDEO_START)는 더 이상 수집하지 않고(START→END 페어링 비용 제거),
-- 멈춤 신호 VIDEO_END 를 VIDEO_PAUSE 로 의미 전환해 멈춘 위치만 집계한다.

-- 1) 전환을 위해 ENUM 에 VIDEO_PAUSE 를 임시로 함께 허용
ALTER TABLE learning_events
    MODIFY COLUMN event_type ENUM ('LECTURE_COMPLETE', 'LECTURE_ENTER', 'LECTURE_EXIT', 'VIDEO_END', 'VIDEO_START', 'VIDEO_PAUSE') NOT NULL;

-- 2) 기존 VIDEO_END(멈춤 위치) 행을 VIDEO_PAUSE 로 이관
UPDATE learning_events SET event_type = 'VIDEO_PAUSE' WHERE event_type = 'VIDEO_END';

-- 3) 더 이상 쓰지 않는 VIDEO_START 행 제거
DELETE FROM learning_events WHERE event_type = 'VIDEO_START';

-- 4) ENUM 을 최종 집합으로 정리
ALTER TABLE learning_events
    MODIFY COLUMN event_type ENUM ('LECTURE_COMPLETE', 'LECTURE_ENTER', 'LECTURE_EXIT', 'VIDEO_PAUSE') NOT NULL;
