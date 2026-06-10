package com.team08.backend.domain.learningevent.repository;

import com.team08.backend.domain.learningevent.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
}
