package com.team08.backend.domain.study.exception;

public class StudyNotEditableException extends RuntimeException {
    public StudyNotEditableException() {
        super("수정할 수 없는 상태입니다.");
    }
}
