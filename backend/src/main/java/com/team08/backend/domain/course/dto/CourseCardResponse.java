package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.global.util.FileUrlFormatter;

public record CourseCardResponse(
        Long id,
        Long instructorId,
        Long categoryId,
        String title,
        String thumbnail,
        int price,
        int viewCount
) {
    public static CourseCardResponse from(Course course, FileUrlFormatter fileUrlFormatter) {
        return new CourseCardResponse(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                fileUrlFormatter.formatThumbnailUrl(course.getThumbnail()),
                course.getPrice(),
                course.getViewCount()
        );
    }
}