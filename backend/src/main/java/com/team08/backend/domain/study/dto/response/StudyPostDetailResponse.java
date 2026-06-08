package com.team08.backend.domain.study.dto.response;

import com.team08.backend.domain.post.entity.Post;
import com.team08.backend.domain.post.entity.PostType;

import java.time.LocalDateTime;
import java.util.List;

public record StudyPostDetailResponse(
        Long postId,
        Long studyId,
        Long userId,
        String nickname,
        String title,
        String content,
        PostType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<StudyCommentResponse> comments
) {
    public static StudyPostDetailResponse from(Post post, List<StudyCommentResponse> comments) {
        return new StudyPostDetailResponse(
                post.getId(),
                post.getStudy().getId(),
                post.getWriter().getId(),
                post.getWriter().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getType(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                comments
        );
    }
}
