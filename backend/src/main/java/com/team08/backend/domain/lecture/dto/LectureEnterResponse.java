package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;

public record LectureEnterResponse(
        Long lectureId,
        String title,
        String m3u8Path,
        String videoUuid,
        int durationSeconds,
        int orderNo,
        Long chapterId,
        LectureProgressSummary progress   // null 이면 학습 이력 없음
) {
    public static LectureEnterResponse of(Lecture lecture, LectureProgress progress) {
        return new LectureEnterResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getM3u8Path(),
                lecture.getVideoUuid(),
                lecture.getDurationSeconds(),
                lecture.getOrderNo(),
                lecture.getChapter().getId(),
                progress != null ? LectureProgressSummary.from(progress) : null
        );
    }
}