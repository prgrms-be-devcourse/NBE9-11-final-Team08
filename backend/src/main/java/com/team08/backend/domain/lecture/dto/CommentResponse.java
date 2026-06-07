package com.team08.backend.domain.lecture.dto;

import com.team08.backend.domain.lecture.entity.LectureComment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "강의 댓글 응답")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long id,
        @Schema(description = "부모 댓글 ID", example = "12")
        Long parentId,
        @Schema(description = "작성자 ID", example = "1")
        Long userId,
        @Schema(description = "작성자 닉네임", example = "테스트유저")
        String userName,
        @Schema(description = "댓글 내용", example = "이 부분에서 Bean 생명주기를 다시 보면 좋겠네요.")
        String content,
        @Schema(description = "댓글이 참조하는 영상 시점(초)", example = "180")
        Integer timestampSeconds,
        @Schema(description = "작성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
    public static CommentResponse from(LectureComment comment) {
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        return new CommentResponse(
                comment.getId(),
                parentId,
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getTimestampSeconds(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
