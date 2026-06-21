package com.team08.backend.domain.media.event;

public record VideoRollbackEvent(
        Long lectureId,
        String targetDirName
) {}