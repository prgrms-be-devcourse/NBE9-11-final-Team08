package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.course.entity.Course;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record CourseDetailResponse(
        Long id,
        String title,
        String description,
        String thumbnail,
        Integer price,
        String categoryName,
        List<ChapterResponse> chapters
) {
    public static CourseDetailResponse from(Course course) {
        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getThumbnail(),
                course.getPrice(),
                getCategoryName(course.getCategory()),
                course.getChapters().stream()
                        .map(chapter -> new ChapterResponse(
                                chapter.getTitle(),
                                chapter.getOrderNo(),
                                chapter.getLectures().stream()
                                        .map(lecture -> new LectureResponse(
                                                lecture.getTitle(),
                                                lecture.getYoutubeVideoId(),
                                                lecture.getDurationSeconds(),
                                                lecture.getIsFreePreview()
                                        )).collect(Collectors.toList())
                        )).collect(Collectors.toList())
        );
    }

    private static String getCategoryName(Category category) {
        return Optional.ofNullable(category)
                .map(Category::getName)
                .orElse("미분류");
    }

    public record ChapterResponse(String title, Integer orderNo, List<LectureResponse> lectures) {}
    public record LectureResponse(String title, String youtubeVideoId, Integer durationSeconds, Boolean isFreePreview) {}
}