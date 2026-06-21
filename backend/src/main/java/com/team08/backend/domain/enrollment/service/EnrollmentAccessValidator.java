package com.team08.backend.domain.enrollment.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//수강권 기반 접근 검증 전용 컴포넌트
// 다른 검증 기능 사용 시 내부 주입만 바꾸면 됨
@Component
@RequiredArgsConstructor
public class EnrollmentAccessValidator {

    private final EnrollmentQueryService enrollmentQueryService;

    public void validateActiveEnrollment(Long userId, Long courseId) {
        if (!enrollmentQueryService.hasActiveEnrollment(userId, courseId)) {
            throw new CustomException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
        }
    }
}
