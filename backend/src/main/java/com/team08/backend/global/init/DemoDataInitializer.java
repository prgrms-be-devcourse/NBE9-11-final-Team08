package com.team08.backend.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * demo 모드 데이터 — "이름만 봐도 어떤 케이스인지 보이는" 페르소나 중심의 엣지/예외 쇼케이스.
 * <p>
 * 1) {@link DataSeeder#seedPersonaShowcase()} 로 의미있는 이름의 소수 계정(판매자 5·수강생 9·관리자)을
 *    각자 한 가지 상태/흐름을 또렷이 보여주도록 손으로 구성하고,
 * 2) 수강(ACTIVE)한 학습자를 그 강좌 스터디(ACTIVE/READONLY)의 MEMBER 로 연결(JdbcTemplate)한다.
 * <p>
 * 풍부한 일반 데이터(강사5·강좌10·수강생20)는 {@link SimpleDataInitializer}(simple 모드)가 담당한다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer {

    private final DataSeeder dataSeeder;
    private final JdbcTemplate jdbcTemplate;

    public void init() {
        // 이미 데이터가 있으면 쇼케이스가 건너뛰고 false 를 반환한다. 그 경우 멤버 연결도 건너뛴다.
        if (!dataSeeder.seedPersonaShowcase()) {
            log.info("[DataInit] 이미 데이터가 존재해 멤버 연결을 건너뜀");
            return;
        }
        linkLearnersAsStudyMembers();
    }

    /**
     * 수강 등록(ACTIVE)된 수강생을 그 강좌 스터디의 MEMBER 로 연결한다(멱등).
     * ACTIVE/READONLY 스터디에만 연결한다(DRAFT/INACTIVE 제외). 환불(CANCELED)은 자연 제외.
     */
    private void linkLearnersAsStudyMembers() {
        int linkedMembers = jdbcTemplate.update("""
                INSERT INTO study_members (study_id, user_id, role, status, joined_at)
                SELECT s.id, e.user_id, 'MEMBER', 'ACTIVE', NOW()
                FROM studies s
                JOIN enrollments e ON e.course_id = s.course_id AND e.status = 'ACTIVE'
                WHERE s.status IN ('ACTIVE', 'READONLY')
                  AND NOT EXISTS (
                    SELECT 1 FROM study_members sm
                    WHERE sm.study_id = s.id AND sm.user_id = e.user_id
                )
                """);

        log.info("[DataInit] 페르소나 쇼케이스: 스터디 멤버 {}건 연결", linkedMembers);
    }
}
