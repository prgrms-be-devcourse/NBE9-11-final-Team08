package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.chapter.dto.ChapterInfoResponse;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.global.util.FileUrlFormatter;
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
    public static CourseDetailResponse from(Course course, FileUrlFormatter fileUrlFormatter) {
        List<ChapterInfoResponse> chapterResponses = course.getChapters().stream()
                .map(ChapterInfoResponse::from)
                .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                course.getDescription(),
                fileUrlFormatter.formatThumbnailUrl(course.getThumbnail()),
                course.getPrice(),
                course.getStatus(),
                course.getViewCount(),
                chapterResponses
        );
    }
}