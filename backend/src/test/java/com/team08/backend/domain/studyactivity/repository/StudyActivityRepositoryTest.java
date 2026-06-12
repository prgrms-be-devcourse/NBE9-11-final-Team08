package com.team08.backend.domain.studyactivity.repository;

import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class StudyActivityRepositoryTest {

    @Autowired
    private StudyActivityRepository studyActivityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 미삭제_활동만_생성일과_ID_역순으로_조회한다() {
        StudyActivity oldest = saveActivity(1L, 1L, "가".repeat(20));
        StudyActivity lowerId = saveActivity(1L, 2L, "나".repeat(20));
        StudyActivity higherId = saveActivity(1L, 3L, "다".repeat(20));
        StudyActivity deleted = saveActivity(1L, 4L, "라".repeat(20));
        saveActivity(2L, 5L, "마".repeat(20));

        LocalDateTime oldTime = LocalDateTime.of(2026, 6, 11, 20, 0);
        LocalDateTime latestTime = LocalDateTime.of(2026, 6, 12, 20, 0);
        updateCreatedAt(oldest.getId(), oldTime);
        updateCreatedAt(lowerId.getId(), latestTime);
        updateCreatedAt(higherId.getId(), latestTime);
        markDeleted(deleted.getId());
        entityManager.clear();

        Page<StudyActivity> result =
                studyActivityRepository.findAllByStudyIdAndDeletedAtIsNull(
                        1L,
                        PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.desc("id")
                                )
                        )
                );

        assertThat(result.getContent())
                .extracting(StudyActivity::getId)
                .containsExactly(higherId.getId(), lowerId.getId(), oldest.getId());
    }

    private StudyActivity saveActivity(Long studyId, Long authorId, String content) {
        return studyActivityRepository.saveAndFlush(
                StudyActivity.create(studyId, authorId, content)
        );
    }

    private void updateCreatedAt(Long activityId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE study_activities SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                activityId
        );
    }

    private void markDeleted(Long activityId) {
        jdbcTemplate.update(
                "UPDATE study_activities SET deleted_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.of(2026, 6, 12, 21, 0)),
                activityId
        );
    }
}
