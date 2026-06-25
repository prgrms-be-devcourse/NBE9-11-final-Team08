package com.team08.backend.domain.media.event;

public record VideoCleanUpEvent(
        Long lectureId,
        String targetDirName
) {}
