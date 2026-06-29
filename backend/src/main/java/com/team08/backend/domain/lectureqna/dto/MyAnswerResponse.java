package com.team08.backend.domain.lectureqna.dto;

import java.time.LocalDateTime;

/**
 * 마이페이지 "작성한 답변" 화면용 응답(강사/판매자).
 * 내가 작성한 QnA 답변을 전 강의에 걸쳐 모아, 답변 대상 질문과 강의/강좌 제목과 함께 내려준다.
 */
public record MyAnswerResponse(
        Long answerId,
        Long questionId,
        Long lectureId,
        String courseTitle,
        String lectureTitle,
        String questionTitle,
        String questionContent,
        String answerContent,
        LocalDateTime createdAt
) {
}
