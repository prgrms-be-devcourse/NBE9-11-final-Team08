package com.team08.backend.domain.chapter.dto;

import com.team08.backend.domain.lecture.entity.Lecture;

public record LectureSummaryResponse(
        Long id,
        String title,
        int orderNo,
        int durationSeconds,
        boolean isFreePreview
) {
    public static LectureSummaryResponse from(Lecture lecture) {
        return new LectureSummaryResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getOrderNo(),
                lecture.getDurationSeconds(),
                lecture.isFreePreview()
        );
    }
}
