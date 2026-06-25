package com.team08.backend.domain.studyreport.repository;

import com.team08.backend.domain.studyreport.entity.StudyDailyStat;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * (user, course, date) 일별 롤업 UPSERT 회귀 테스트.
 * 같은 키로 반복 호출 시 1행만 유지되며 event_count/completed_count 가 누적되는지 실제 MySQL(Testcontainers)에서 검증한다.
 */
@DataJpaTest
@Import(JpaConfig.class)
class StudyDailyStatRepositoryTest {

    @Autowired
    private StudyDailyStatRepository repository;

    private static final LocalDate DAY = LocalDate.of(2026, 6, 25);

    @Test
    @DisplayName("같은 (user, course, date) upsert 반복 시 1행만 유지되고 카운트가 누적된다")
    void upsert_sameKey_accumulates() {
        repository.upsertIncrement(1L, 100L, DAY, 0); // 일반 이벤트
        repository.upsertIncrement(1L, 100L, DAY, 0);
        repository.upsertIncrement(1L, 100L, DAY, 1); // 완료 이벤트

        List<StudyDailyStat> rows = repository.findByUserIdAndCourseIdOrderByActivityDateAsc(1L, 100L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEventCount()).isEqualTo(3);
        assertThat(rows.get(0).getCompletedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("날짜가 다르면 별도 행으로 쌓이고 날짜 오름차순으로 조회된다")
    void upsert_differentDates_separateRowsOrdered() {
        repository.upsertIncrement(1L, 100L, DAY.plusDays(1), 1);
        repository.upsertIncrement(1L, 100L, DAY, 0);

        List<StudyDailyStat> rows = repository.findByUserIdAndCourseIdOrderByActivityDateAsc(1L, 100L);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getActivityDate()).isEqualTo(DAY);
        assertThat(rows.get(1).getActivityDate()).isEqualTo(DAY.plusDays(1));
    }

    @Test
    @DisplayName("다른 (user, course) 의 롤업은 섞이지 않는다")
    void upsert_isolatedByUserAndCourse() {
        repository.upsertIncrement(1L, 100L, DAY, 1);
        repository.upsertIncrement(2L, 100L, DAY, 1); // 다른 유저
        repository.upsertIncrement(1L, 200L, DAY, 1); // 다른 강좌

        assertThat(repository.findByUserIdAndCourseIdOrderByActivityDateAsc(1L, 100L)).hasSize(1);
    }
}
