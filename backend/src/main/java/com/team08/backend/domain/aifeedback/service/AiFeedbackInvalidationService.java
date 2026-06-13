package com.team08.backend.domain.aifeedback.service;

import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.repository.AiFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiFeedbackInvalidationService implements AiFeedbackInvalidator {

    private final AiFeedbackRepository aiFeedbackRepository;

    @Override
    @Transactional
    public void markStale(Long studyActivityId) {
        aiFeedbackRepository.findByStudyActivityId(studyActivityId)
                .ifPresent(AiFeedback::markStale);
    }
}
