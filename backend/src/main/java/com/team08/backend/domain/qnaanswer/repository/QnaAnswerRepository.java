package com.team08.backend.domain.qnaanswer.repository;

import com.team08.backend.domain.qnaanswer.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
}
