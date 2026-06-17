package com.team08.backend.domain.lecturemodificationrequest.repository;

import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LectureModificationRequestRepository extends JpaRepository<LectureModificationRequest, Long> {

    @Query("SELECT r FROM LectureModificationRequest r " +
            "JOIN FETCH r.lecture l " +
            "WHERE r.id = :requestId")
    Optional<LectureModificationRequest> findByIdWithLecture(@Param("requestId") Long requestId);
}