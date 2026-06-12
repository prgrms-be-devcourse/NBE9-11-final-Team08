package com.team08.backend.domain.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CourseUpdateRequest(
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
        String thumbnail,

        @Valid
        @NotNull(message = "커리큘럼(챕터) 목록은 필수입니다.")
        List<ChapterUpdateRequest> chapters
) {
    public record ChapterUpdateRequest(
            Long id,

            @NotBlank(message = "챕터 제목은 필수입니다.")
            @Size(max = 255, message = "챕터 제목은 255자 이하로 입력해주세요.")
            String title,

            @Min(value = 1, message = "순서는 1 이상이어야 합니다.")
            int orderNo,

            @Valid
            @NotNull(message = "강의 목록은 필수입니다.")
            List<LectureUpdateRequest> lectures
    ) {}

    public record LectureUpdateRequest(
            Long id,

            @NotBlank(message = "강의 제목은 필수입니다.")
            @Size(max = 255, message = "강의 제목은 255자 이하로 입력해주세요.")
            String title,

            @Min(value = 0, message = "영상 길이는 0초 이상이어야 합니다.")
            int durationSeconds,

            @Min(value = 1, message = "순서는 1 이상이어야 합니다.")
            int orderNo,

            boolean isFreePreview
            // 영상 파일 경로(m3u8Path)는 심사 대상이므로 일반 수정에서 제외
    ) {}
}