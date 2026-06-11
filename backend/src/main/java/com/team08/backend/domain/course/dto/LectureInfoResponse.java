package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.lecture.entity.Lecture;

public record LectureInfoResponse(
        Long id,
        String title,
        String m3u8Path,
        int durationSeconds,
        int orderNo,
        boolean isFreePreview
) {
    public static LectureInfoResponse from(Lecture lecture) {
        return new LectureInfoResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.isFreePreview() ? lecture.getM3u8Path() : null,
                lecture.getDurationSeconds(),
                lecture.getOrderNo(),
                lecture.isFreePreview()
        );
    }
}