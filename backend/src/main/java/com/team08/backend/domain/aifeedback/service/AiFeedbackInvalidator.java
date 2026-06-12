package com.team08.backend.domain.aifeedback.service;

public interface AiFeedbackInvalidator {

    void markStale(Long studyActivityId);
}
