package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.dto.MyAnswerResponse;
import com.team08.backend.domain.lectureqna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
    Optional<QnaAnswer> findByQuestionId(Long questionId);
    boolean existsByQuestionId(Long questionId);
    List<QnaAnswer> findByQuestionIdIn(List<Long> questionIds);

    /**
     * 내가(강사) 작성한 답변을 대상 질문·강의·강좌 제목과 함께 최신순으로 조회한다.
     * QnaAnswer~QnaQuestion~Lecture~Course 를 명시적으로 조인한다.
     */
    @Query("""
            SELECT new com.team08.backend.domain.lectureqna.dto.MyAnswerResponse(
                a.id, q.id, q.lectureId, c.title, l.title, q.title, q.content, a.content, a.createdAt)
            FROM QnaAnswer a, QnaQuestion q, Lecture l
                JOIN l.chapter ch
                JOIN ch.course c
            WHERE a.questionId = q.id
              AND q.lectureId = l.id
              AND a.instructorId = :userId
              AND q.deletedAt IS NULL
            ORDER BY a.createdAt DESC
            """)
    List<MyAnswerResponse> findMyAnswers(@Param("userId") Long userId);
}
