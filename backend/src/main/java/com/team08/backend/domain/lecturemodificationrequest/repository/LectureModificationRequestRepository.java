package com.team08.backend.domain.lecturemodificationrequest.repository;

import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureModificationRequestRepository extends JpaRepository<LectureModificationRequest, Long> {
}
