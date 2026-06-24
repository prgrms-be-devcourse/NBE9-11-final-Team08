package com.team08.backend.domain.chapter.dto;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.dto.LectureInfoResponse;

import java.util.List;

public record ChapterInfoResponse(
        Long id,
        String title,
        int orderNo,
        List<LectureInfoResponse> lectures
) {
    public static ChapterInfoResponse from(Chapter chapter) {
        List<LectureInfoResponse> lectureResponses = chapter.getLectures().stream()
                .map(LectureInfoResponse::from)
                .toList();

        return new ChapterInfoResponse(
                chapter.getId(),
                chapter.getTitle(),
                chapter.getOrderNo(),
                lectureResponses
        );
    }
}