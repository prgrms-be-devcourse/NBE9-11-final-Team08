package com.team08.backend.domain.course.dto;

public record LectureSaveDto(
        String videoId,
        String title,
        Integer durationSeconds,
        Integer orderNo,
        Boolean isFreePreview
) {
}