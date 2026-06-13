package com.team08.backend.domain.study.command;

public record CourseStudyCreateCommand(
        Long ownerId,
        Long courseId,
        String title,
        String description
) {
}
