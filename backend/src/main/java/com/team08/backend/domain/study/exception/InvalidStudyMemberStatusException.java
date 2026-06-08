package com.team08.backend.domain.study.exception;

public class InvalidStudyMemberStatusException extends RuntimeException {
    public InvalidStudyMemberStatusException() {
        super("처리할 수 없는 멤버 상태입니다.");
    }
}
