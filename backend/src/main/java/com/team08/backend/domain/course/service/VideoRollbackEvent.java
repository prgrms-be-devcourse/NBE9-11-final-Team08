package com.team08.backend.domain.course.service;

public record VideoRollbackEvent(
        Long lectureId,
        String targetDirName
) {}