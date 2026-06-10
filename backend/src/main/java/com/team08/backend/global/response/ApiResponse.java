package com.team08.backend.global.response;


import com.team08.backend.global.exception.ErrorCode;

public record ApiResponse<T>(
        String code,
        String message,
        T result
) {

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>("200-1", "요청 성공", result);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}