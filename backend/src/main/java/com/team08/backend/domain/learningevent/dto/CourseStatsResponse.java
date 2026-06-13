package com.team08.backend.domain.learningevent.dto;

public record CourseStatsResponse(
        Long courseId,
        long enterCount,
        long watchTimeSeconds,
        long completionCount
) {}
