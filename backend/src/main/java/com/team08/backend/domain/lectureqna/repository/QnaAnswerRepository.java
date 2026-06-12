package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
    Optional<QnaAnswer> findByQuestionId(Long questionId);
    boolean existsByQuestionId(Long questionId);
    List<QnaAnswer> findByQuestionIdIn(List<Long> questionIds);
}
