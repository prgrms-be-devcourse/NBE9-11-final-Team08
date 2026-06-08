package com.team08.backend.domain.study.exception;

public class StudyCommentNotFoundException extends RuntimeException {
    public StudyCommentNotFoundException() {
        super("댓글을 찾을 수 없습니다.");
    }
}
