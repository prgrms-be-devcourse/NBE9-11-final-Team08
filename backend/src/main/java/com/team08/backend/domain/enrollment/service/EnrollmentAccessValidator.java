package com.team08.backend.domain.enrollment.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 수강권 기반 접근 검증 전용 컴포넌트.
 * <p>
 * "수강권 보유 여부 조회"({@link EnrollmentQueryService})와 "접근 거부 정책(어떤 예외를 던질지)"을
 * 한 곳에 묶어, 이를 사용하는 도메인 서비스들이 {@link EnrollmentQueryService} 에 직접 의존하지 않도록 한다.
 * 향후 수강권 조회 방식이 바뀌어도 소비자(ChapterService, LectureService 등)는 영향받지 않는다.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentAccessValidator {

    private final EnrollmentQueryService enrollmentQueryService;

    /** 해당 강좌에 ACTIVE 수강권이 없으면 접근을 거부한다. */
    public void validateActiveEnrollment(Long userId, Long courseId) {
        if (!enrollmentQueryService.hasActiveEnrollment(userId, courseId)) {
            throw new CustomException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
        }
    }
}
