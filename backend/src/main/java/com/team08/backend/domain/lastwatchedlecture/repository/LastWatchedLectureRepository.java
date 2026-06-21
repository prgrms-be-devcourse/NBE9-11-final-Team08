package com.team08.backend.domain.lastwatchedlecture.repository;

import com.team08.backend.domain.lastwatchedlecture.entity.LastWatchedLecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LastWatchedLectureRepository extends JpaRepository<LastWatchedLecture, Long> {

    // (user_id, course_id) 유니크 키 단건 조회 — 강좌별 마지막 시청 강의
    Optional<LastWatchedLecture> findByUserIdAndCourseId(Long userId, Long courseId);
}
