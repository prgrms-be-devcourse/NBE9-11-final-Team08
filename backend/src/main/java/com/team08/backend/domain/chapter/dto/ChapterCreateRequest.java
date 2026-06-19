package com.team08.backend.domain.chapter.dto;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterCreateRequest(
        @NotBlank(message = "챕터 제목은 필수입니다.")
        @Size(max = 255, message = "챕터 제목은 255자 이하로 입력해주세요.")
        String title,

        @NotNull(message = "챕터 순서는 필수입니다.")
        int orderNo
) {
    public Chapter toEntity(Course course) {
        return Chapter.create(this.title, this.orderNo, course);
    }
}