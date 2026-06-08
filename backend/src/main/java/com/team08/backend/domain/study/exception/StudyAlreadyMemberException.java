package com.team08.backend.domain.study.exception;

public class StudyAlreadyMemberException extends RuntimeException {
    public StudyAlreadyMemberException() {
        super("이미 참여 중인 스터디입니다.");
    }
}
