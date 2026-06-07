package com.team08.backend.domain.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "강의 댓글 작성 요청")
public record CommentCreateRequest(
        @Schema(description = "댓글 내용", example = "이 부분에서 Bean 생명주기를 다시 보면 좋겠네요.", minLength = 1, maxLength = 1000, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 1000)
        String content,

        @Schema(description = "대댓글인 경우 부모 댓글 ID", example = "12", minimum = "1")
        @Positive
        Long parentId,

        @Schema(description = "댓글이 참조하는 영상 시점(초)", example = "180", minimum = "0")
        @Min(0)
        Integer timestampSeconds
) {
}
