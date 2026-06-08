package com.team08.backend.domain.study.exception;

public class InvalidStudyStatusTransitionException extends RuntimeException {
    public InvalidStudyStatusTransitionException() {
        super("잘못된 스터디 상태 변경");
    }
}
