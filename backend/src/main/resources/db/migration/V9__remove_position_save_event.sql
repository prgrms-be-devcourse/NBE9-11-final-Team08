-- POSITION_SAVE 이벤트 제거.
-- 하트비트(시청 진행)는 PATCH /api/lectures/{lectureId}/progress 전용 엔드포인트로 대체되어
-- learning_events 에는 더 이상 적재되지 않는다. (LearningEventType 에서 POSITION_SAVE 삭제)
-- 기존 행을 먼저 정리한 뒤, event_type ENUM 에서 POSITION_SAVE 를 제거한다.
DELETE FROM learning_events WHERE event_type = 'POSITION_SAVE';

ALTER TABLE learning_events
    MODIFY COLUMN event_type ENUM ('LECTURE_COMPLETE', 'LECTURE_ENTER', 'LECTURE_EXIT', 'VIDEO_END', 'VIDEO_START') NOT NULL;
