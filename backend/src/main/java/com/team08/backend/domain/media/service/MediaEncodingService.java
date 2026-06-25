package com.team08.backend.domain.media.service;

import java.io.File;

public interface MediaEncodingService {
    void encodeToHls(File file, String targetDirName, Long lectureId);

    void encodeModificationToHls(File file, String targetDirName, Long lectureId, String description, Long instructorId);

    void deleteEncodedFolder(String targetDirName, Long lectureId);
}