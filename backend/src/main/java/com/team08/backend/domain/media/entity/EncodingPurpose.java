package com.team08.backend.domain.media.entity;

import lombok.Getter;

@Getter
public enum EncodingPurpose {
    CREATE("신규 강의 생성"),
    MODIFY("기존 강의 영상 수정 요청");

    private final String description;

    EncodingPurpose(String description) {
        this.description = description;
    }
}