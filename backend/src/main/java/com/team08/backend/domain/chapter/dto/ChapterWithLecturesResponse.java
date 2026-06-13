package com.team08.backend.domain.chapter.dto;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;

import java.util.Comparator;
import java.util.List;

public record ChapterWithLecturesResponse(
        Long id,
        String title,
        int orderNo,
        List<LectureSummaryResponse> lectures
) {
    public static ChapterWithLecturesResponse from(Chapter chapter) {
        List<LectureSummaryResponse> lectures = chapter.getLectures().stream()
                .sorted(Comparator.comparingInt(Lecture::getOrderNo))
                .map(LectureSummaryResponse::from)
                .toList();

        return new ChapterWithLecturesResponse(
                chapter.getId(),
                chapter.getTitle(),
                chapter.getOrderNo(),
                lectures
        );
    }
}
