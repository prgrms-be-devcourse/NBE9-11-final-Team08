package com.team08.backend.global.init;

import com.team08.backend.global.init.DataSeeder.SeedConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 프론트 시연용(demo) 데이터.
 * <p>
 * simple/bulk 와 동일한 데이터 그래프를 소~중량으로 만들되(아래 CONFIG),
 * 추가로 "수강생을 자신이 수강한 강좌의 스터디에 MEMBER 로 연결"한다.
 * 그래야 일반 학습자 계정으로 로그인했을 때 내 스터디 + 강좌 + 강의가 모두 보인다.
 * <p>
 * 스터디 owner 는 규칙대로 SELLER 그대로 두고, 멤버 연결과 강좌 노출(ON_SALE)만 보강한다.
 * 도메인 엔티티(StudyMember 등)는 건드리지 않고 JdbcTemplate 으로만 보강한다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer {

    /** 시연용: 강사 3 · 강좌 6(3x2) · 챕터 3 · 영상 3 · 수강생 12 · 쿠폰정책 3 */
    private static final SeedConfig CONFIG = new SeedConfig(3, 2, 3, 3, 12, 3, 500);

    private final DataSeeder dataSeeder;
    private final JdbcTemplate jdbcTemplate;

    public void init() {
        dataSeeder.seed(CONFIG);
        linkLearnersAsStudyMembers();
    }

    /**
     * 각 스터디의 강좌에 수강 등록(ACTIVE)된 수강생을 그 스터디의 MEMBER 로 연결한다.
     * 이미 멤버면 건너뛴다(멱등). 시연 편의를 위해 해당 강좌를 ON_SALE 로도 노출한다.
     * <p>
     * 기존 데이터가 있어 seed() 가 스킵되더라도 이 보강은 매 기동 시 멱등하게 적용된다.
     */
    private void linkLearnersAsStudyMembers() {
        int openedCourses = jdbcTemplate.update("""
                UPDATE courses c
                JOIN studies s ON s.course_id = c.id
                SET c.status = 'ON_SALE', c.updated_at = NOW()
                WHERE c.status <> 'ON_SALE'
                """);

        int linkedMembers = jdbcTemplate.update("""
                INSERT INTO study_members (study_id, user_id, role, status, joined_at)
                SELECT s.id, e.user_id, 'MEMBER', 'ACTIVE', NOW()
                FROM studies s
                JOIN enrollments e ON e.course_id = s.course_id AND e.status = 'ACTIVE'
                WHERE NOT EXISTS (
                    SELECT 1 FROM study_members sm
                    WHERE sm.study_id = s.id AND sm.user_id = e.user_id
                )
                """);

        log.info("[DataInit] 시연용 보강: 강좌 {}건 ON_SALE, 스터디 멤버 {}건 연결",
                openedCourses, linkedMembers);
    }
}
