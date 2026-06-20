package com.team08.backend.domain.lectureprogress.repository;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class LectureProgressRepositoryTest {

    @Autowired
    private LectureProgressRepository lectureProgressRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // findByUserIdAndLectureId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자와 강의 ID로 진행 정보를 조회한다")
    void findByUserIdAndLectureId_returnsProgress() {
        save(1L, 10L, 120, false, time(10, 0));

        Optional<LectureProgress> result =
                lectureProgressRepository.findByUserIdAndLectureId(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getLastPositionSeconds()).isEqualTo(120);
    }

    @Test
    @DisplayName("다른 사용자의 진행 정보는 조회되지 않는다")
    void findByUserIdAndLectureId_otherUser_returnsEmpty() {
        save(1L, 10L, 120, false, time(10, 0));

        Optional<LectureProgress> result =
                lectureProgressRepository.findByUserIdAndLectureId(2L, 10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("진행 정보가 없으면 빈 Optional을 반환한다")
    void findByUserIdAndLectureId_notExists_returnsEmpty() {
        Optional<LectureProgress> result =
                lectureProgressRepository.findByUserIdAndLectureId(99L, 99L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("강좌 내 강의 중 가장 최근 학습한 진행 정보를 반환한다")
    void findTopByUserIdAndLectureIdIn_returnsMostRecent() {
        save(1L, 10L, 100, false, time(10, 0));
        save(1L, 11L, 200, false, time(12, 0)); // 가장 최근
        save(1L, 12L, 50, false, time(9, 0));

        Optional<LectureProgress> result =
                lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(
                        1L, List.of(10L, 11L, 12L));

        assertThat(result).isPresent();
        assertThat(result.get().getLectureId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("목록에 포함된 강의만 대상으로 가장 최근 진행 정보를 반환한다")
    void findTopByUserIdAndLectureIdIn_filtersByLectureIds() {
        save(1L, 10L, 100, false, time(10, 0));
        save(1L, 99L, 999, false, time(23, 0)); // 더 최근이지만 목록에 없음

        Optional<LectureProgress> result =
                lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(
                        1L, List.of(10L));

        assertThat(result).isPresent();
        assertThat(result.get().getLectureId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("학습한 강의가 없으면 빈 Optional을 반환한다")
    void findTopByUserIdAndLectureIdIn_noMatch_returnsEmpty() {
        save(1L, 10L, 100, false, time(10, 0));

        Optional<LectureProgress> result =
                lectureProgressRepository.findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(
                        1L, List.of(50L, 51L));

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private LocalDateTime time(int hour, int minute) {
        return LocalDateTime.of(2026, 6, 13, hour, minute);
    }

    private LectureProgress save(Long userId, Long lectureId, Integer lastPositionSeconds,
                                  Boolean completed, LocalDateTime updatedAt) {
        return lectureProgressRepository.saveAndFlush(new LectureProgress(
                null,
                lectureId,
                userId,
                lastPositionSeconds,
                lastPositionSeconds,
                50,
                completed,
                completed ? updatedAt : null,
                updatedAt,
                updatedAt
        ));
    }
}
