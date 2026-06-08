package com.team08.backend.domain.study.exception;

public class InvalidStudyApplicationStatusException extends RuntimeException {
    public InvalidStudyApplicationStatusException() {
        super("이미 처리된 참여 신청입니다.");
    }
}
