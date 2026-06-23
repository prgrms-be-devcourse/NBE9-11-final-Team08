package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.lecture.entity.Lecture;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LectureCreateRequest(
        @NotBlank(message = "강의 제목은 필수입니다.")
        @Size(max = 255, message = "강의 제목은 255자 이하로 입력해주세요.")
        String title,

        String m3u8Path,

        String videoUuid,

        String summary,

        @Min(value = 1, message = "강의 재생 시간은 최소 1초 이상이어야 합니다.")
        int durationSeconds,

        @NotNull(message = "강의 순서는 필수입니다.")
        int orderNo,

        boolean isFreePreview
) {
    public Lecture toEntity(Chapter chapter) {
        if (this.m3u8Path == null || this.m3u8Path.isBlank()) {
            return Lecture.createDraft(
                    this.title,
                    this.summary,
                    this.durationSeconds,
                    this.orderNo,
                    this.isFreePreview,
                    chapter
            );
        }
        return Lecture.createWithStream(
                this.m3u8Path,
                this.videoUuid,
                this.title,
                this.summary,
                this.durationSeconds,
                this.orderNo,
                this.isFreePreview,
                chapter
        );
    }
}