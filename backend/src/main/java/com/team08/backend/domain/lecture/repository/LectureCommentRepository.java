package com.team08.backend.domain.lecture.repository;

import com.team08.backend.domain.lecture.entity.LectureComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureCommentRepository extends JpaRepository<LectureComment, Long> {

    //특정강의에 대한 댓글 불러오기
    List<LectureComment> findByLectureIdAndDeletedFalseOrderByCreatedAtAsc(Long lectureId);

    //새로운 댓글 불러오기
    List<LectureComment> findByLectureIdAndIdGreaterThanAndDeletedFalseOrderByCreatedAtAsc(Long lectureId, Long afterId);

    //스터디당 작성한 댓글 수
    long countByUserIdAndLectureChapterCourseIdAndDeletedFalse(Long userId, Long courseId);
}
