package com.team08.backend.domain.media.dto;

import com.team08.backend.domain.media.entity.EncodingPurpose;

public record EncodingContext(
        Long lectureId,
        String dbSavePath,
        String targetDirName,
        EncodingPurpose purpose,
        String description,
        Long instructorId
) {}