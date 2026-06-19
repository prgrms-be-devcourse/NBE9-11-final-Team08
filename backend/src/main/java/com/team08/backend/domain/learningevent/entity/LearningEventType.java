package com.team08.backend.domain.learningevent.entity;

public enum LearningEventType {
    LECTURE_ENTER,  //강의 입장
    VIDEO_START,    //영상 시작 및 일시정지 후 재개
    VIDEO_END,      //영상 멈춤 및 재생 중단
    POSITION_SAVE,  //heartBeat
    LECTURE_COMPLETE//수강 완료
}
