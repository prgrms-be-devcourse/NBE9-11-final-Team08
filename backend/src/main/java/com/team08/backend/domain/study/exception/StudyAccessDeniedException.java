package com.team08.backend.domain.study.exception;

public class StudyAccessDeniedException extends RuntimeException {
    public StudyAccessDeniedException() {
        super("권한이 없습니다.");
    }
}
