package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

/**
 * 마이페이지 "작성한 댓글" 화면용 응답.
 * 내가 작성한 QnA 질문을 전 강의에 걸쳐 모아 강의/강좌 제목과 함께 내려준다.
 */
public record MyCommentResponse(
        Long id,
        Long lectureId,
        String courseTitle,
        String lectureTitle,
        String title,
        String content,
        LocalDateTime createdAt,
        boolean answered
) {
}
