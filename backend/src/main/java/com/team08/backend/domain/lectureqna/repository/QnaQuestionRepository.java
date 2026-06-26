package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.dto.MyQnaRow;
import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {
    Page<QnaQuestion> findByLectureIdAndDeletedAtIsNull(Long lectureId, Pageable pageable);
    Optional<QnaQuestion> findByIdAndDeletedAtIsNull(Long id);
    long countByUserIdAndLectureIdIn(Long userId, List<Long> lectureIds);

    /**
     * 내가 작성한(삭제되지 않은) QnA 질문을 강의/강좌 제목과 함께 최신순으로 조회한다.
     * QnaQuestion 은 lectureId 만 보관하므로 Lecture~Course 와 명시적으로 조인한다.
     */
    @Query("""
            SELECT new com.team08.backend.domain.lectureqna.dto.MyQnaRow(
                q.id, q.lectureId, c.title, l.title, q.title, q.content, q.createdAt)
            FROM QnaQuestion q, Lecture l
                JOIN l.chapter ch
                JOIN ch.course c
            WHERE q.lectureId = l.id
              AND q.userId = :userId
              AND q.deletedAt IS NULL
            ORDER BY q.createdAt DESC
            """)
    List<MyQnaRow> findMyComments(@Param("userId") Long userId);
}
