package com.team08.backend.domain.study.dto.request;

import com.team08.backend.domain.post.entity.PostType;

public record StudyPostUpdateRequest(
        String title,
        String content,
        PostType type
) {
}
