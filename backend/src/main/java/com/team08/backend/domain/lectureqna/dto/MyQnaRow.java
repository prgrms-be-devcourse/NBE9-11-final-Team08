package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

/**
 * 내가 작성한 QnA 질문 + 강의/강좌 제목을 평탄화한 조회 전용 projection.
 * 답변 여부(answered)는 별도로 조회해 {@link MyCommentResponse}로 합친다.
 */
public record MyQnaRow(
        Long id,
        Long lectureId,
        String courseTitle,
        String lectureTitle,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
