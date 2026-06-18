package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class QnaQuestionRepositoryTest {

    @Autowired
    private QnaQuestionRepository qnaQuestionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // findByLectureIdAndDeletedAtIsNull
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("강의별 삭제되지 않은 질문만 페이지로 반환한다")
    void findByLectureIdAndDeletedAtIsNull_returnsOnlyNotDeleted() {
        save(1L, 10L);
        save(1L, 10L);
        QnaQuestion deleted = save(1L, 10L);
        deleted.delete();
        qnaQuestionRepository.saveAndFlush(deleted);
        save(1L, 99L); // 다른 강의

        Page<QnaQuestion> result = qnaQuestionRepository
                .findByLectureIdAndDeletedAtIsNull(10L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(q -> q.getLectureId().equals(10L) && q.getDeletedAt() == null);
    }

    @Test
    @DisplayName("질문이 없는 강의는 빈 페이지를 반환한다")
    void findByLectureIdAndDeletedAtIsNull_noQuestions_returnsEmpty() {
        Page<QnaQuestion> result = qnaQuestionRepository
                .findByLectureIdAndDeletedAtIsNull(123L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIdAndDeletedAtIsNull
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ID로 삭제되지 않은 질문을 조회한다")
    void findByIdAndDeletedAtIsNull_returnsQuestion() {
        QnaQuestion saved = save(1L, 10L);

        Optional<QnaQuestion> result =
                qnaQuestionRepository.findByIdAndDeletedAtIsNull(saved.getId());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("삭제된 질문은 ID로 조회되지 않는다")
    void findByIdAndDeletedAtIsNull_deleted_returnsEmpty() {
        QnaQuestion saved = save(1L, 10L);
        saved.delete();
        qnaQuestionRepository.saveAndFlush(saved);

        Optional<QnaQuestion> result =
                qnaQuestionRepository.findByIdAndDeletedAtIsNull(saved.getId());

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countByUserIdAndLectureIdIn
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("사용자가 강의 목록에 작성한 질문 수를 집계한다")
    void countByUserIdAndLectureIdIn_countsMatching() {
        save(1L, 10L);
        save(1L, 11L);
        save(1L, 99L); // 목록에 없는 강의
        save(2L, 10L); // 다른 사용자

        long count = qnaQuestionRepository.countByUserIdAndLectureIdIn(1L, List.of(10L, 11L));

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("작성한 질문이 없으면 0을 반환한다")
    void countByUserIdAndLectureIdIn_noMatch_returnsZero() {
        save(1L, 10L);

        long count = qnaQuestionRepository.countByUserIdAndLectureIdIn(1L, List.of(50L));

        assertThat(count).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private QnaQuestion save(Long userId, Long lectureId) {
        return qnaQuestionRepository.saveAndFlush(
                QnaQuestion.create(userId, lectureId, "제목", "내용"));
    }
}
