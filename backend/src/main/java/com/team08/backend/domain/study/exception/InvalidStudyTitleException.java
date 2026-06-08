package com.team08.backend.domain.study.exception;

public class InvalidStudyTitleException extends RuntimeException {
    public InvalidStudyTitleException() {
        super("스터디 제목은 빈값을 허용하지 않습니다.");
    }
}
