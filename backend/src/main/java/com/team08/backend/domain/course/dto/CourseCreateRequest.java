package com.team08.backend.domain.course.dto;

import com.team08.backend.domain.course.entity.CourseStatus;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseCreateRequest {

    @NotBlank(message = "강좌 제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "강좌 설명은 필수입니다.")
    private String description;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private int price;

    @NotBlank(message = "썸네일 이미지 경로는 필수입니다.")
    private String thumbnail;

    @NotNull(message = "강좌 상태는 필수입니다.")
    private CourseStatus status;

    public CourseCreateRequest(String title, String description, Long categoryId,
                               int price, String thumbnail, CourseStatus status) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.price = price;
        this.thumbnail = thumbnail;
        this.status = status;
    }
}