package com.team08.backend.domain.qnaquestion.repository;

import com.team08.backend.domain.qnaquestion.entity.QnaQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {
}
