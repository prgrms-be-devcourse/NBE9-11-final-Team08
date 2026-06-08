package com.team08.backend.domain.instructor.repository;

import com.team08.backend.domain.instructor.entity.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {
    Optional<InstructorProfile> findByUserId(Long userId);
    boolean existsByUserIdAndApprovedAtIsNotNull(Long userId);
}