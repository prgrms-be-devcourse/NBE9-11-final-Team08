package com.team08.backend.domain.learningevent.repository;

import com.team08.backend.domain.learningevent.dto.CourseStatsProjection;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class LearningEventRepositoryTest {

    @Autowired
    private LearningEventRepository learningEventRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // 중복 이벤트 방지 - existsByUniqueEventKey
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("저장된 키는 중복으로 감지한다")
    void existsByUniqueEventKey_existingKey_returnsTrue() {
        save(1L, 10L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "key-001");

        assertThat(learningEventRepository.existsByUniqueEventKey("key-001")).isTrue();
    }

    @Test
    @DisplayName("없는 키는 중복이 아니다")
    void existsByUniqueEventKey_notExistingKey_returnsFalse() {
        assertThat(learningEventRepository.existsByUniqueEventKey("unknown-key")).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 사용자별 활동 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자별 이벤트만 페이지로 반환한다")
    void findByUserId_returnsOnlyTargetUserEvents() {
        save(1L, 10L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "k1");
        save(1L, 10L, 2L, 3L, LearningEventType.VIDEO_PAUSE, 100, "k2");
        save(2L, 10L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "k3"); // 다른 유저

        Page<LearningEvent> result =
                learningEventRepository.findByUserId(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(e -> e.getUserId().equals(1L));
    }

    @Test
    @DisplayName("이벤트가 없는 사용자는 빈 페이지를 반환한다")
    void findByUserId_noEvents_returnsEmpty() {
        Page<LearningEvent> result =
                learningEventRepository.findByUserId(99L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 강의별 통계 - getStatsByCourseId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("단일 쿼리로 강좌별 입장 수, 시청 시간, 완료 수를 한 번에 집계한다")
    void getStatsByCourseId_aggregatesAllInOneQuery() {
        Long courseId = 10L;
        save(1L, courseId, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "e1");
        save(2L, courseId, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "e2");
        save(3L, courseId, 2L, 3L, LearningEventType.VIDEO_PAUSE, 300, "e3");
        save(4L, courseId, 2L, 3L, LearningEventType.VIDEO_PAUSE, 500, "e4");
        save(5L, courseId, 2L, 3L, LearningEventType.LECTURE_COMPLETE, null, "e5");
        save(6L, 99L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "e6"); // 다른 강좌 제외

        CourseStatsProjection result = learningEventRepository.getStatsByCourseId(courseId);

        assertThat(result.enterCount()).isEqualTo(2L);
        assertThat(result.watchTimeSeconds()).isEqualTo(800L);
        assertThat(result.completionCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이벤트가 없는 강좌는 모든 통계가 0이다")
    void getStatsByCourseId_noEvents_returnsAllZero() {
        CourseStatsProjection result = learningEventRepository.getStatsByCourseId(999L);

        assertThat(result.enterCount()).isEqualTo(0L);
        assertThat(result.watchTimeSeconds()).isEqualTo(0L);
        assertThat(result.completionCount()).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 챕터별 통계 - countByChapterIdAndEventType, avgWatchTimeSeconds
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("챕터별 이벤트 타입 카운트를 정확히 집계한다")
    void countByChapterIdAndEventType_correctCount() {
        Long chapterId = 20L;
        save(1L, 10L, chapterId, 3L, LearningEventType.LECTURE_COMPLETE, null, "c1");
        save(2L, 10L, chapterId, 3L, LearningEventType.LECTURE_COMPLETE, null, "c2");
        save(3L, 10L, 99L, 3L, LearningEventType.LECTURE_COMPLETE, null, "c3"); // 다른 챕터

        assertThat(learningEventRepository.countByChapterIdAndEventType(chapterId, LearningEventType.LECTURE_COMPLETE))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("챕터별 평균 시청 시간을 집계한다")
    void avgWatchTimeSecondsByChapterId_correctAverage() {
        Long chapterId = 20L;
        save(1L, 10L, chapterId, 3L, LearningEventType.VIDEO_PAUSE, 400, "a1");
        save(2L, 10L, chapterId, 3L, LearningEventType.VIDEO_PAUSE, 600, "a2");
        save(3L, 10L, 99L, 3L, LearningEventType.VIDEO_PAUSE, 9999, "a3"); // 다른 챕터 제외

        double avg = learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId);

        assertThat(avg).isEqualTo(500.0);
    }

    @Test
    @DisplayName("VIDEO_PAUSE 이벤트가 없으면 평균 시청 시간은 0이다")
    void avgWatchTimeSecondsByChapterId_noVideoPause_returnsZero() {
        Long chapterId = 20L;
        save(1L, 10L, chapterId, 3L, LearningEventType.LECTURE_ENTER, null, "z1");

        assertThat(learningEventRepository.avgWatchTimeSecondsByChapterId(chapterId)).isEqualTo(0.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 판매자 강좌 필터링 - findByCourseIdIn
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("판매자 강좌 ID 목록에 해당하는 이벤트만 반환한다")
    void findByCourseIdIn_returnsOnlyMatchingCourses() {
        save(1L, 10L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "s1");
        save(2L, 11L, 2L, 3L, LearningEventType.VIDEO_PAUSE, 100, "s2");
        save(3L, 99L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "s3"); // 제외

        Page<LearningEvent> result = learningEventRepository.findByCourseIdIn(
                List.of(10L, 11L), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(e -> List.of(10L, 11L).contains(e.getCourseId()));
    }

    @Test
    @DisplayName("판매자 강좌 ID가 빈 목록이면 빈 페이지를 반환한다")
    void findByCourseIdIn_emptyCourseIds_returnsEmpty() {
        save(1L, 10L, 2L, 3L, LearningEventType.LECTURE_ENTER, null, "empty1");

        Page<LearningEvent> result = learningEventRepository.findByCourseIdIn(
                List.of(), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private LearningEvent save(Long userId, Long courseId, Long chapterId, Long lectureId,
                                LearningEventType eventType, Integer positionSeconds, String key) {
        return learningEventRepository.saveAndFlush(
                LearningEvent.create(userId, courseId, chapterId, lectureId,
                        eventType, positionSeconds,
                        LocalDateTime.of(2026, 6, 13, 10, 0), key)
        );
    }
}
