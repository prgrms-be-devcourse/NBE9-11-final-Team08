package com.team08.backend.domain.media.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String s3Key = "courses/thumbnails/" + courseId + "/" + UUID.randomUUID() + extension;

            File tempFile = File.createTempFile("thumb-", extension);
            file.transferTo(tempFile);

            try {
                return s3FileStorageService.uploadFile(tempFile, s3Key);
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}