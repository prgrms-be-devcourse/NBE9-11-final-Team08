package com.team08.backend.domain.study.exception;

public class InvalidStudyMemberRoleException extends RuntimeException {
    public InvalidStudyMemberRoleException() {
        super("변경할 수 없는 멤버 역할입니다.");
    }
}
