package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import java.util.List;

public record CourseDetailResponse(
        Long id,
        Long instructorId,
        Long categoryId,
        String title,
        String description,
        String thumbnail,
        int price,
        CourseStatus status,
        int viewCount,
        List<ChapterInfoResponse> chapters
) {
    public static CourseDetailResponse from(Course course) {
        List<ChapterInfoResponse> chapterResponses = course.getChapters().stream()
                .map(ChapterInfoResponse::from)
                .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                course.getDescription(),
                course.getThumbnail(),
                course.getPrice(),
                course.getStatus(),
                course.getViewCount(),
                chapterResponses
        );
    }
}