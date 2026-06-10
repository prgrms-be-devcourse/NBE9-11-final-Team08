package com.team08.backend.domain.aifeedback.repository;

import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {
}
