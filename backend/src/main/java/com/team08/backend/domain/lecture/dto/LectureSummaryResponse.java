package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.Lecture;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 기본 정보")
public record LectureSummaryResponse(
        @Schema(description = "강의 ID", example = "1")
        Long id,
        @Schema(description = "챕터 ID", example = "1")
        Long chapterId,
        @Schema(description = "코스 ID", example = "1")
        Long courseId,
        @Schema(description = "강의 제목", example = "스프링 Bean 기초")
        String title,
        @Schema(description = "영상 ID", example = "dQw4w9WgXcQ")
        String videoId,
        @Schema(description = "영상 길이(초)", example = "1200")
        Integer durationSeconds,
        @Schema(description = "챕터 내 강의 순서", example = "1")
        Integer orderNo
) {
    public static LectureSummaryResponse from(Lecture lecture) {
        return new LectureSummaryResponse(
                lecture.getId(),
                lecture.getChapter().getId(),
                lecture.getChapter().getCourse().getId(),
                lecture.getTitle(),
                lecture.getVideoId(),
                lecture.getDurationSeconds(),
                lecture.getOrderNo()
        );
    }
}
