package com.team08.backend.domain.media.event;

public record CourseThumbnailEvent(
        Long courseId,
        String oldThumbnail,
        String newS3Key
) {}