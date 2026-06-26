package com.team08.backend.domain.learningevent.dto;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import jakarta.validation.constraints.NotNull;

public record RecordLearningEventRequest(

        Long courseId,

        Long chapterId,

        @NotNull
        Long lectureId,

        @NotNull
        LearningEventType eventType,

        Integer positionSeconds,

        /**
         * 클라이언트가 생성한 고유 키 (UUID 권장).
         * 동일한 키가 이미 존재하면 중복 이벤트로 처리된다.
         * null 이면 서버가 자동 생성한다.
         */
        String eventKey
) {}
