package com.team08.backend.domain.lecturereflection.repository;

import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class LectureReflectionRepositoryTest {

    @Autowired
    private LectureReflectionRepository lectureReflectionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // existsByUserIdAndLectureId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회고가 존재하면 true를 반환한다")
    void existsByUserIdAndLectureId_existing_returnsTrue() {
        save(1L, 10L);

        assertThat(lectureReflectionRepository.existsByUserIdAndLectureId(1L, 10L)).isTrue();
    }

    @Test
    @DisplayName("회고가 없으면 false를 반환한다")
    void existsByUserIdAndLectureId_notExisting_returnsFalse() {
        save(1L, 10L);

        assertThat(lectureReflectionRepository.existsByUserIdAndLectureId(1L, 99L)).isFalse();
        assertThat(lectureReflectionRepository.existsByUserIdAndLectureId(2L, 10L)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByUserIdAndLectureId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자와 강의 기준으로 회고를 조회한다")
    void findByUserIdAndLectureId_returnsReflection() {
        save(1L, 10L);

        Optional<LectureReflection> result =
                lectureReflectionRepository.findByUserIdAndLectureId(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("회고 내용");
    }

    @Test
    @DisplayName("다른 사용자의 회고는 조회되지 않는다")
    void findByUserIdAndLectureId_otherUser_returnsEmpty() {
        save(1L, 10L);

        Optional<LectureReflection> result =
                lectureReflectionRepository.findByUserIdAndLectureId(2L, 10L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private LectureReflection save(Long userId, Long lectureId) {
        return lectureReflectionRepository.saveAndFlush(
                LectureReflection.create(userId, lectureId, "회고 내용"));
    }
}
