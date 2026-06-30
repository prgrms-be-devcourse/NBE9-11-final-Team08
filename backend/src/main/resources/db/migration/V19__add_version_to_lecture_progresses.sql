-- 하트비트 누적(watchedSeconds += delta)의 lost update 방어용 낙관적 락 버전 컬럼.
-- 같은 (user, lecture) 행에 대한 동시 하트비트가 각자 읽고 더한 뒤 저장하면 한쪽 delta 가
-- 사라진다. @Version 으로 UPDATE 시 version 을 함께 검증/증가시켜 충돌을 감지하고,
-- 서비스 계층이 재조회→재가산으로 재시도한다.
-- 기존 행은 DEFAULT 0 으로 채워지고, 네이티브 insertIfAbsent 는 version = 0 을 명시 삽입한다.
ALTER TABLE lecture_progresses
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
