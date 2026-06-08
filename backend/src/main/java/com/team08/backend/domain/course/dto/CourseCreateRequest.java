package com.team08.backend.domain.course.dto;

public record CourseCreateRequest(
        Long categoryId,
        String title,
        String description,
        String thumbnail,
        Integer price
) {
}