package com.team08.backend.domain.media.service;

import org.springframework.web.multipart.MultipartFile;

public interface CourseThumbnailService {

    String uploadThumbnail(Long courseId, MultipartFile file);

    void deleteThumbnail(String s3Key);
}