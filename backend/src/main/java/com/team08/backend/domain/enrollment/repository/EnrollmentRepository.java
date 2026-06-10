package com.team08.backend.domain.enrollment.repository;

import com.team08.backend.domain.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
}
