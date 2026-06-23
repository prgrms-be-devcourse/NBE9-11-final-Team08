-- =====================================================================
-- 학습/스터디 기능 테스트용 시드 (MySQL, dev DB)
--
-- 목적: "학습자 계정"으로 로그인하면
--   - 내 스터디에 스터디 1개가 보이고 (study_members 에 MEMBER 로 직접 추가)
--   - 그 스터디의 강좌(챕터/강의)가 보이며
--   - 수강권(enrollment ACTIVE)이 있어 강의 입장(enterLecture)·진행률·학습이벤트가 동작한다.
--
-- 규칙: 스터디 owner 는 반드시 SELLER. 학습자는 MEMBER 로만 참여.
--
-- 로그인 계정 (비밀번호 모두 Test1234!):
--   - 학습자: study-user@test.com   (ROLE_USER)   → 이 계정으로 학습/스터디 테스트
--   - 강사  : study-seller@test.com (ROLE_SELLER) → 스터디 owner
--
-- ID 는 기존 데이터와 충돌하지 않도록 800000 대역을 명시 사용한다.
-- 재실행 가능: 맨 위 DELETE 블록이 자기 ID 들을 먼저 정리한다.
-- =====================================================================

START TRANSACTION;

-- ── 재실행을 위한 정리 (child → parent 순서) ───────────────────────────
DELETE FROM study_members WHERE id IN (800001, 800002);
DELETE FROM studies       WHERE id = 800001;
DELETE FROM enrollments   WHERE id = 800001;
DELETE FROM order_items   WHERE id = 800001;
DELETE FROM orders        WHERE id = 800001;
DELETE FROM lectures      WHERE id IN (800001, 800002, 800003, 800004);
DELETE FROM chapters      WHERE id IN (800001, 800002);
DELETE FROM courses       WHERE id = 800001;
DELETE FROM seller        WHERE id = 800001;
DELETE FROM users         WHERE id IN (800001, 800002);
DELETE FROM categories    WHERE id = 800001;

-- ── 카테고리 ──────────────────────────────────────────────────────────
INSERT INTO categories (id, parent_category_id, name, depth)
VALUES (800001, NULL, '백엔드', 1);

-- ── 사용자 (비밀번호 = Test1234! 의 bcrypt 해시) ──────────────────────
INSERT INTO users (id, email, password, nickname, profile_image, role, created_at, updated_at) VALUES
  (800001, 'study-seller@test.com', '$2a$10$R7XjzTfcABYnDoLUklg3HeEa6gJNAnPnpREIT6eLd5YIMgP9bSMuW', '시드강사',   NULL, 'ROLE_SELLER', NOW(), NOW()),
  (800002, 'study-user@test.com',   '$2a$10$R7XjzTfcABYnDoLUklg3HeEa6gJNAnPnpREIT6eLd5YIMgP9bSMuW', '시드학습자', NULL, 'ROLE_USER',   NOW(), NOW());

-- ── 판매자(Seller) 프로필 ────────────────────────────────────────────
INSERT INTO seller (id, user_id, created_at, updated_at)
VALUES (800001, 800001, NOW(), NOW());

-- ── 강좌 (ON_SALE: 카탈로그/스터디에서 정상 노출) ────────────────────
INSERT INTO courses (id, instructor_id, category_id, title, description, thumbnail, price, status, view_count, deleted_at, created_at, updated_at)
VALUES (800001, 800001, 800001, 'SQL 시드 강좌 - 백엔드 입문',
        'SQL 시드로 생성한 테스트 강좌입니다.', 'thumb-seed.jpg', 20000, 'ON_SALE', 0, NULL, NOW(), NOW());

-- ── 챕터 ──────────────────────────────────────────────────────────────
INSERT INTO chapters (id, title, order_no, deleted_at, course_id, created_at, updated_at) VALUES
  (800001, '1장. 오리엔테이션', 1, NULL, 800001, NOW(), NOW()),
  (800002, '2장. 영속성과 JPA', 2, NULL, 800001, NOW(), NOW());

-- ── 강의 (각 챕터 2개, 1강은 무료 미리보기) ──────────────────────────
INSERT INTO lectures (id, m3u8path, title, summary, duration_seconds, order_no, is_free_preview, deleted_at, chapter_id, created_at, updated_at) VALUES
  (800001, '/hls/800001/1/index.m3u8', '강좌 소개',        '강좌 전체 흐름 소개',   600,  1, 1, NULL, 800001, NOW(), NOW()),
  (800002, '/hls/800001/2/index.m3u8', '개발 환경 설정',   '로컬 환경 구성',        900,  2, 0, NULL, 800001, NOW(), NOW()),
  (800003, '/hls/800002/1/index.m3u8', '영속성이란',       '영속성 개념',          1200,  1, 0, NULL, 800002, NOW(), NOW()),
  (800004, '/hls/800002/2/index.m3u8', 'JPA 매핑 기초',    '엔티티 매핑',          1500,  2, 0, NULL, 800002, NOW(), NOW());

-- ── 주문/주문항목 (PAID): 수강권 FK + "구매한 강좌" 노출용 ───────────
INSERT INTO orders (id, user_id, order_number, total_price, discount_price, final_price, status,
                    ordered_at, paid_at, canceled_at, refunded_at, expired_at, created_at, updated_at)
VALUES (800001, 800002, 'ORD-SQL-SEED-1', 20000, 0, 20000, 'PAID',
        NOW(), NOW(), NULL, NULL, NULL, NOW(), NOW());

INSERT INTO order_items (id, order_id, course_id, course_title, price, discount_price, final_price, created_at, updated_at)
VALUES (800001, 800001, 800001, 'SQL 시드 강좌 - 백엔드 입문', 20000, 0, 20000, NOW(), NOW());

-- ── 수강권(Enrollment ACTIVE): enterLecture 권한 통과에 필수 ─────────
INSERT INTO enrollments (id, user_id, course_id, order_id, status,
                         enrolled_at, canceled_at, expired_at, created_at, updated_at)
VALUES (800001, 800002, 800001, 800001, 'ACTIVE', NOW(), NULL, NULL, NOW(), NULL);

-- ── 스터디 (owner = SELLER, ACTIVE) ──────────────────────────────────
INSERT INTO studies (id, course_id, user_id, title, description, status, created_at, updated_at)
VALUES (800001, 800001, 800001, 'SQL 시드 스터디', '학습/스터디 기능 테스트용 스터디', 'ACTIVE', NOW(), NOW());

-- ── 스터디 멤버: owner(강사) + member(학습자) ───────────────────────
INSERT INTO study_members (id, study_id, user_id, role, status, joined_at, left_at, kicked_at) VALUES
  (800001, 800001, 800001, 'OWNER',  'ACTIVE', NOW(), NULL, NULL),
  (800002, 800001, 800002, 'MEMBER', 'ACTIVE', NOW(), NULL, NULL);

COMMIT;
