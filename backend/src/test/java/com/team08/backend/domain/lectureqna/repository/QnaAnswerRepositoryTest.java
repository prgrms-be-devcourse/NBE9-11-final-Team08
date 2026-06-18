package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class QnaAnswerRepositoryTest {

    @Autowired
    private QnaAnswerRepository qnaAnswerRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // findByQuestionId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("질문 ID로 답변을 조회한다")
    void findByQuestionId_returnsAnswer() {
        save(100L, 10L, "답변 내용");

        Optional<QnaAnswer> result = qnaAnswerRepository.findByQuestionId(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("답변 내용");
    }

    @Test
    @DisplayName("답변이 없는 질문은 빈 Optional을 반환한다")
    void findByQuestionId_noAnswer_returnsEmpty() {
        Optional<QnaAnswer> result = qnaAnswerRepository.findByQuestionId(999L);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // existsByQuestionId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("답변이 존재하는 질문은 true를 반환한다")
    void existsByQuestionId_existing_returnsTrue() {
        save(100L, 10L, "답변 내용");

        assertThat(qnaAnswerRepository.existsByQuestionId(100L)).isTrue();
    }

    @Test
    @DisplayName("답변이 없는 질문은 false를 반환한다")
    void existsByQuestionId_notExisting_returnsFalse() {
        assertThat(qnaAnswerRepository.existsByQuestionId(999L)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByQuestionIdIn
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("질문 ID 목록에 해당하는 답변들을 한 번에 조회한다")
    void findByQuestionIdIn_returnsMatching() {
        save(100L, 10L, "답변1");
        save(101L, 10L, "답변2");
        save(102L, 10L, "답변3"); // 목록에 없음

        List<QnaAnswer> result = qnaAnswerRepository.findByQuestionIdIn(List.of(100L, 101L));

        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(a -> List.of(100L, 101L).contains(a.getQuestionId()));
    }

    @Test
    @DisplayName("일치하는 답변이 없으면 빈 목록을 반환한다")
    void findByQuestionIdIn_noMatch_returnsEmpty() {
        save(100L, 10L, "답변1");

        List<QnaAnswer> result = qnaAnswerRepository.findByQuestionIdIn(List.of(500L, 501L));

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // fixture
    // ─────────────────────────────────────────────────────────────────────────

    private QnaAnswer save(Long questionId, Long instructorId, String content) {
        return qnaAnswerRepository.saveAndFlush(
                QnaAnswer.create(questionId, instructorId, content));
    }
}
