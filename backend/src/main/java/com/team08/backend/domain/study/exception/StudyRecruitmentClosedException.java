package com.team08.backend.domain.study.exception;

public class StudyRecruitmentClosedException extends RuntimeException {
    public StudyRecruitmentClosedException() {
        super("모집 중인 스터디가 아닙니다.");
    }
}
