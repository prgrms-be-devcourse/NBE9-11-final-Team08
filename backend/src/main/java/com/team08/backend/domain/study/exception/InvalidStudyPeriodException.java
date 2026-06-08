package com.team08.backend.domain.study.exception;

public class InvalidStudyPeriodException extends RuntimeException {
    public InvalidStudyPeriodException() {
        super("스터디 종료일자는 시작일자 이후여야 합니다.");
    }
}
