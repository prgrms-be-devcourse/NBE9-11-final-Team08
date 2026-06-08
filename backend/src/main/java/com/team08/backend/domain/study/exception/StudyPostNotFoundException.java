package com.team08.backend.domain.study.exception;

public class StudyPostNotFoundException extends RuntimeException {
    public StudyPostNotFoundException() {
        super("게시글을 찾을 수 없습니다.");
    }
}
