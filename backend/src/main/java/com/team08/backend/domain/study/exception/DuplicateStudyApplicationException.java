package com.team08.backend.domain.study.exception;

public class DuplicateStudyApplicationException extends RuntimeException {
    public DuplicateStudyApplicationException() {
        super("이미 참여 신청한 스터디입니다.");
    }
}
