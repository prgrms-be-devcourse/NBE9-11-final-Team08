package com.team08.backend.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "API 오류 응답")
public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,
        @Schema(description = "오류 코드", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "오류 메시지", example = "요청 값이 올바르지 않습니다.")
        String message,
        @Schema(description = "필드별 검증 오류 목록")
        List<FieldErrorResponse> fieldErrors,
        @Schema(description = "오류 발생 시각")
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String code, String message, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(status, code, message, fieldErrors, LocalDateTime.now());
    }

    @Schema(description = "필드 검증 오류")
    public record FieldErrorResponse(
            @Schema(description = "오류 필드 또는 파라미터명", example = "content")
            String field,
            @Schema(description = "거절된 값", example = "")
            String rejectedValue,
            @Schema(description = "검증 실패 사유", example = "공백일 수 없습니다")
            String reason
    ) {
    }
}
