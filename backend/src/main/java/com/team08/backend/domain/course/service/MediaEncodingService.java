package com.team08.backend.domain.course.service;

import java.io.File;

public interface MediaEncodingService {
    void encodeToHls(File sourceFile, String targetDirName, Long lectureId);
}