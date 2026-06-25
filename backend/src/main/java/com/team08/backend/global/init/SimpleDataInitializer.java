package com.team08.backend.global.init;

import com.team08.backend.global.init.DataSeeder.SeedConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 일반 개발/시연용 데이터.
 * <p>
 * 1) {@link DataSeeder#seed} 로 데이터 그래프를 소~중량으로 만들고,
 * 2) {@link DataSeeder#seedDemoScenarios()} 로 강좌 생명주기·스터디 상태·환불·AI 코치·학습 흐름·
 *    QnA·영상 수정요청·출석·쿠폰·레포트를 다양한 상태로 펼치고(멤버 보강 포함),
 * 3) 수강(ACTIVE)한 학습자를 그 강좌 스터디(ACTIVE/READONLY)의 MEMBER 로 연결한다.
 * <p>
 * 엣지/예외 케이스를 "의미있는 이름"으로 또렷이 보여주는 쪽은 {@link DemoDataInitializer}(demo 모드)다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SimpleDataInitializer {

    /** 강사 6 · 강좌 18(6x3) · 챕터 4 · 영상 3 · 수강생 80 · 쿠폰정책 5 (강좌당 수강생 다수 → 대시보드/리포트 가시성) */
    private static final SeedConfig CONFIG = new SeedConfig(6, 3, 4, 3, 80, 5, 500);

    private final DataSeeder dataSeeder;
    private final JdbcTemplate jdbcTemplate;

    public void init() {
        // 이미 데이터가 있으면 seed() 가 건너뛰고 false 를 반환한다.
        // 그 경우 비멱등인 시나리오 보강도 함께 건너뛴다.
        if (!dataSeeder.seed(CONFIG)) {
            log.info("[DataInit] 이미 데이터가 존재해 시나리오 보강을 건너뜀");
            return;
        }
        dataSeeder.seedDemoScenarios();
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

        log.info("[DataInit] 스터디 멤버 {}건 연결", linkedMembers);
    }
}
