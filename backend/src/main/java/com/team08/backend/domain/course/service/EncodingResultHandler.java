package com.team08.backend.domain.course.service;

public interface EncodingResultHandler {
    void handleSuccess(Long lectureId, String dbSavePath, String targetDirName, String description, Long instructorId);
}