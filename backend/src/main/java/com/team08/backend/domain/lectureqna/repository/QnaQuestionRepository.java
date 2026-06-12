package com.team08.backend.domain.lectureqna.repository;

import com.team08.backend.domain.lectureqna.entity.QnaQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {
    Page<QnaQuestion> findByLectureIdAndDeletedAtIsNull(Long lectureId, Pageable pageable);
    Optional<QnaQuestion> findByIdAndDeletedAtIsNull(Long id);
    long countByUserIdAndLectureIdIn(Long userId, List<Long> lectureIds);
}
