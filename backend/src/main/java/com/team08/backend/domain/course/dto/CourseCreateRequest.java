package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import jakarta.validation.constraints.*;

public record CourseCreateRequest(
        @NotBlank(message = "강좌 제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
        String title,

        @NotBlank(message = "강좌 설명은 필수입니다.")
        String description,

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        int price,

        @NotBlank(message = "썸네일 이미지 경로는 필수입니다.")
        String thumbnail
) {
    public Course toEntity(Long instructorId) {
        return Course.builder()
                .instructorId(instructorId)
                .categoryId(this.categoryId)
                .title(this.title)
                .description(this.description)
                .thumbnail(this.thumbnail)
                .price(this.price)
                .status(CourseStatus.DRAFT)
                .build();
    }
}