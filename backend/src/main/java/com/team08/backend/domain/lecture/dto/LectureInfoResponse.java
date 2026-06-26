package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.Lecture;

public record LectureInfoResponse(
        Long id,
        String title,
        String summary,
        String m3u8Path,
        String videoUuid,
        int durationSeconds,
        int orderNo,
        boolean isFreePreview,
        boolean hasVideo
) {
    public static LectureInfoResponse from(Lecture lecture) {
        boolean hasVideo = lecture.getM3u8Path() != null && !lecture.getM3u8Path().isBlank();
        return new LectureInfoResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getSummary(),
                lecture.isFreePreview() ? lecture.getM3u8Path() : null,
                lecture.getVideoUuid(),
                lecture.getDurationSeconds(),
                lecture.getOrderNo(),
                lecture.isFreePreview(),
                hasVideo
        );
    }
}