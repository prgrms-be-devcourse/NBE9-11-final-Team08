package com.team08.backend.domain.dashboard.exception;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;

/**
 * 관리자 대시보드 도메인 전용 예외.
 * 공통 {@link CustomException}을 직접 던지지 않고 상속해 도메인 의미를 드러낸다.
 * 부모 타입이므로 {@code GlobalExceptionHandler}가 그대로 처리한다.
 */
public class DashboardException extends CustomException {

    public DashboardException(ErrorCode errorCode) {
        super(errorCode);
    }
}
