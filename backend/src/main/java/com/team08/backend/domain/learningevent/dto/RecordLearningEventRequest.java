package com.team08.backend.domain.learningevent.dto;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RecordLearningEventRequest(

        Long courseId,

        Long chapterId,

        @NotNull
        Long lectureId,

        @NotNull
        LearningEventType eventType,

        Integer positionSeconds,

        /**
         * 이벤트 발생 시각. 타임존 모호성을 없애기 위해 오프셋을 포함한 ISO-8601 로 받는다.
         * 예: "2026-06-13T10:00:00+09:00". 서버는 이를 KST 기준으로 변환해 저장한다.
         */
        @NotNull
        OffsetDateTime eventTime,

        /**
         * 클라이언트가 생성한 고유 키 (UUID 권장).
         * 동일한 키가 이미 존재하면 중복 이벤트로 처리된다.
         * null 이면 서버가 자동 생성한다.
         */
        String eventKey
) {}
