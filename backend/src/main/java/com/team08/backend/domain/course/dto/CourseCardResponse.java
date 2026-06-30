package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.global.util.FileUrlFormatter;

public record CourseCardResponse(
        Long id,
        Long instructorId,
        Long categoryId,
        String title,
        String thumbnail,
        int price,
        int viewCount,
        CourseStatus status,
        String statusReason
) {
    public CourseCardResponse(
            Long id,
            Long instructorId,
            Long categoryId,
            String title,
            String thumbnail,
            int price,
            int viewCount
    ) {
        this(id, instructorId, categoryId, title, thumbnail, price, viewCount, null, null);
    }

    public static CourseCardResponse from(Course course, FileUrlFormatter fileUrlFormatter) {
        return from(course, fileUrlFormatter, null);
    }

    public static CourseCardResponse from(Course course, FileUrlFormatter fileUrlFormatter, String statusReason) {
        return new CourseCardResponse(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                fileUrlFormatter.formatThumbnailUrl(course.getThumbnail()),
                course.getPrice(),
                course.getViewCount(),
                course.getStatus(),
                statusReason
        );
    }
}
