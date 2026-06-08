package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record StudyCommentResponse(
        Long commentId,
        Long postId,
        Long userId,
        String nickname,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyCommentResponse from(Comment comment) {
        return new StudyCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getDisplayContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
