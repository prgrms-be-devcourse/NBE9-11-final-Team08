package com.team08.backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseReviewSubmitRequest(
        @Schema(description = "심사 요청 사유 및 관리자 전달 메시지", example = "강좌 내용 수정을 완료하여 심사를 요청합니다.")
        String reason
) {
}