package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.post.entity.Post;
import com.team08.backend.domain.post.entity.PostType;

import java.time.LocalDateTime;

public record StudyPostSummaryResponse(
        Long postId,
        Long studyId,
        Long userId,
        String nickname,
        String title,
        PostType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyPostSummaryResponse from(Post post) {
        return new StudyPostSummaryResponse(
                post.getId(),
                post.getStudy().getId(),
                post.getWriter().getId(),
                post.getWriter().getNickname(),
                post.getTitle(),
                post.getType(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
