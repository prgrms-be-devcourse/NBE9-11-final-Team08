package com.team08.backend.domain.lecturereflection.dto;

import com.team08.backend.domain.lecturereflection.entity.LectureReflection;

public record LectureReflectionResponse(
        Long id,
        Long userId,
        Long lectureId,
        String content
) {

    public static LectureReflectionResponse from(LectureReflection reflection) {
        return new LectureReflectionResponse(
                reflection.getId(),
                reflection.getUserId(),
                reflection.getLectureId(),
                reflection.getContent()
        );
    }
}