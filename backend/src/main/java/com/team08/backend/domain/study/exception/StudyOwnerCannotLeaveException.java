package com.team08.backend.domain.study.exception;

public class StudyOwnerCannotLeaveException extends RuntimeException {
    public StudyOwnerCannotLeaveException() {
        super("스터디 생성자는 탈퇴할 수 없습니다.");
    }
}
