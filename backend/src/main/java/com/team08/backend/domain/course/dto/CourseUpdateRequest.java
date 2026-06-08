package com.team08.backend.domain.course.dto;

public record CourseUpdateRequest(
        Long categoryId,
        String title,
        String description,
        String thumbnail,
        Integer price
) {
}