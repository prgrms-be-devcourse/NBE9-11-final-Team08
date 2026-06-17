package com.team08.backend.domain.course.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaEncodingService {
    void encodeToHls(MultipartFile file, String targetDirName, Long lectureId);

    void encodeModificationToHls(MultipartFile file, String targetDirName, Long lectureId, String description);
}