package com.team08.backend.domain.study.exception;

public class StudyApplicationNotFoundException extends RuntimeException {
    public StudyApplicationNotFoundException() {
        super("참여 신청을 찾을 수 없습니다.");
    }
}
