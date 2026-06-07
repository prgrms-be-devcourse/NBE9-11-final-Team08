package com.team08.backend.domain.study.exception;

public class StudyOwnerCannotBeKickedException extends RuntimeException {
    public StudyOwnerCannotBeKickedException() {
        super("스터디 생성자는 강퇴할 수 없습니다.");
    }
}
