package com.team08.backend.domain.learningevent.entity;

public enum LearningEventType {
    LECTURE_ENTER,  //강의 입장
    VIDEO_PAUSE,    //영상 멈춤(일시정지/중단) — 멈춘 위치는 학습자가 어려워한 구간 신호
    LECTURE_EXIT,   //강의 퇴장 (마지막 시청 위치를 LectureProgress 에 저장)
    LECTURE_COMPLETE//수강 완료
}
