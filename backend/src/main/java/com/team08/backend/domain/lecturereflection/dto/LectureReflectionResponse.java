package com.team08.backend.domain.lecturereflection.dto;

import com.team08.backend.domain.lecturereflection.entity.LectureReflection;

import java.time.LocalDateTime;

public record LectureReflectionResponse(
        Long id,
        Long userId,
        Long lectureId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LectureReflectionResponse from(LectureReflection r) {
        return new LectureReflectionResponse(
                r.getId(), r.getUserId(), r.getLectureId(),
                r.getContent(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
