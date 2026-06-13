package com.team08.backend.domain.learningevent.dto;

import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;

import java.time.LocalDateTime;

public record LearningEventResponse(
        Long id,
        Long userId,
        Long courseId,
        Long chapterId,
        Long lectureId,
        LearningEventType eventType,
        Integer positionSeconds,
        LocalDateTime eventTime
) {
    public static LearningEventResponse from(LearningEvent event) {
        return new LearningEventResponse(
                event.getId(),
                event.getUserId(),
                event.getCourseId(),
                event.getChapterId(),
                event.getLectureId(),
                event.getEventType(),
                event.getPositionSeconds(),
                event.getEventTime()
        );
    }
}
