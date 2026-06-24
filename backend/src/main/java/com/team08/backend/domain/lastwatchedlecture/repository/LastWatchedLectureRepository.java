package com.team08.backend.domain.lastwatchedlecture.repository;

import com.team08.backend.domain.lastwatchedlecture.entity.LastWatchedLecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LastWatchedLectureRepository extends JpaRepository<LastWatchedLecture, Long> {

    // (user_id, course_id) 유니크 키 단건 조회 — 강좌별 마지막 시청 강의
    Optional<LastWatchedLecture> findByUserIdAndCourseId(Long userId, Long courseId);

    // 강좌별 마지막 시청 강의 원자적 UPSERT.
    // 강의 입장 동시 호출 시 (user_id, course_id) 유니크 충돌(Duplicate entry)을 피하기 위해
    // find-or-save 대신 INSERT ... ON DUPLICATE KEY UPDATE 로 한 번에 처리한다.
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO last_watched_lectures (user_id, course_id, lecture_id, created_at, updated_at) "
            + "VALUES (:userId, :courseId, :lectureId, NOW(), NOW()) AS new "
            + "ON DUPLICATE KEY UPDATE lecture_id = new.lecture_id, updated_at = NOW()",
            nativeQuery = true)
    void upsert(@Param("userId") Long userId,
                @Param("courseId") Long courseId,
                @Param("lectureId") Long lectureId);
}
