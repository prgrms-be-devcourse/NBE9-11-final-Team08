package com.team08.backend.domain.study.exception;

public class StudyMemberNotFoundException extends RuntimeException {
    public StudyMemberNotFoundException() {
        super("스터디 멤버를 찾을 수 없습니다.");
    }
}
