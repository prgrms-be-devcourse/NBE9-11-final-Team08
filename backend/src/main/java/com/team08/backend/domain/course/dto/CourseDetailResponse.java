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
        String statusReason,
        int viewCount,
        List<ChapterInfoResponse> chapters
) {
    public static CourseDetailResponse from(Course course, FileUrlFormatter fileUrlFormatter) {
        return from(course, course.getViewCount(), fileUrlFormatter, null);
    }

    public static CourseDetailResponse from(Course course, int viewCount, FileUrlFormatter fileUrlFormatter) {
        return from(course, viewCount, fileUrlFormatter, null);
    }

    public static CourseDetailResponse from(Course course, int viewCount, FileUrlFormatter fileUrlFormatter, String statusReason) {
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
                statusReason,
                viewCount,
                chapterResponses
        );
    }

    public CourseDetailResponse withViewCount(int newViewCount) {
        return new CourseDetailResponse(
                this.id,
                this.instructorId,
                this.categoryId,
                this.title,
                this.description,
                this.thumbnail,
                this.price,
                this.status,
                this.statusReason,
                newViewCount,
                this.chapters
        );
    }

    public CourseDetailResponse withStatusReason(String newStatusReason) {
        return new CourseDetailResponse(
                this.id,
                this.instructorId,
                this.categoryId,
                this.title,
                this.description,
                this.thumbnail,
                this.price,
                this.status,
                newStatusReason,
                this.viewCount,
                this.chapters
        );
    }
}
