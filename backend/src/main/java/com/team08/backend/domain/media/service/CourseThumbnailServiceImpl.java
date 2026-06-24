package com.team08.backend.domain.media.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseThumbnailServiceImpl implements CourseThumbnailService {

    private final S3FileStorageService s3FileStorageService;

    @Override
    public String uploadThumbnail(Long courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try (InputStream is = file.getInputStream()) {
            String extension = getFileExtension(file);
            String s3Key = "courses/thumbnails/" + courseId + "/" + UUID.randomUUID() + extension;

            return s3FileStorageService.uploadFile(is, s3Key);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteThumbnail(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }
        try {
            s3FileStorageService.deleteFile(s3Key);
        } catch (Exception e) {
            log.error("Failed to delete thumbnail from S3. key: {}", s3Key, e);
        }
    }

    private String getFileExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.equals("image/jpeg")) return ".jpg";
            if (contentType.equals("image/gif")) return ".gif";
            if (contentType.equals("image/webp")) return ".webp";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}