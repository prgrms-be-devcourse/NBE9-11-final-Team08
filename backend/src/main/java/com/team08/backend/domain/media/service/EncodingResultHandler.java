package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.EncodingPurpose;

public interface EncodingResultHandler {
    void handleSuccess(Long lectureId, String dbSavePath, String targetDirName,
                       EncodingPurpose purpose, String description, Long instructorId);
}