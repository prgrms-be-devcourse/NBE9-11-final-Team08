-- 스터디 리포트 스키마 개편 (3개 변경을 하나로):
--  1) study_reports: progress_rate(파생값) 제거, 대신 완료/전체 강의 수를 보관
--     - progress_rate 는 completed_lectures/total_lectures 의 이행 종속 파생값이라 저장하지 않는다.
--     - completed/total 은 강의 완료 이벤트마다 증분 재집계되어 /studies 목록 진행도의 단일 소스가 된다.
--  2) learning_daily_stats: (사용자, 강좌, 날짜)별 학습 활동 일별 롤업 신설
--     - 학습 이벤트마다 UPSERT 로 증분되어, 리포트의 학습일수·일별진도·일별활동맵을
--       learning_events GROUP BY 스캔 없이 이 테이블에서 읽는다.
--  3) 기존 learning_events 로부터 롤업 백필(= 재빌드와 동일 집계)

ALTER TABLE study_reports
    ADD COLUMN completed_lectures INT NULL,
    ADD COLUMN total_lectures INT NULL,
    DROP COLUMN progress_rate;

CREATE TABLE learning_daily_stats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    activity_date DATE NOT NULL,
    event_count INT NOT NULL,
    completed_count INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_user_course_date UNIQUE (user_id, course_id, activity_date)
) ENGINE=InnoDB;

INSERT INTO learning_daily_stats (user_id, course_id, activity_date, event_count, completed_count)
SELECT user_id,
       course_id,
       DATE(event_time) AS activity_date,
       COUNT(*) AS event_count,
       SUM(CASE WHEN event_type = 'LECTURE_COMPLETE' THEN 1 ELSE 0 END) AS completed_count
FROM learning_events
WHERE course_id IS NOT NULL
GROUP BY user_id, course_id, DATE(event_time);
