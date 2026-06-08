package com.team08.backend.domain.study.exception;

public class StudyKickedMemberCannotApplyException extends RuntimeException {
    public StudyKickedMemberCannotApplyException() {
        super("강퇴된 사용자는 다시 참여 신청할 수 없습니다.");
    }
}
