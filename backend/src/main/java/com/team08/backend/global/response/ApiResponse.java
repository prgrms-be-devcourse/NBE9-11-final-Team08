package com.team08.backend.global.response;

import com.team08.backend.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        String code,
        String message,
        T result
) {

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>("200", "요청 성공", result);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}
