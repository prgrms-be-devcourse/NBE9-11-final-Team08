package com.team08.backend.global.util;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStorageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Template s3Template;

    public String uploadFile(File file, String s3Key) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            s3Template.upload(bucket, s3Key, is);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3. key: {}", s3Key, e);
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }
    
    public File downloadFile(String s3Key, File destinationFile) {
        try (InputStream inputStream = s3Template.download(bucket, s3Key).getInputStream()) {
            Files.copy(inputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destinationFile;
        } catch (Exception e) {
            log.error("Failed to download file from S3. key: {}", s3Key, e);
            throw new CustomException(ErrorCode.S3_DOWNLOAD_FAILED);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            s3Template.deleteObject(bucket, s3Key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3. key: {}", s3Key, e);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    public void deleteDirectory(String s3Prefix) {
        try {
            s3Template.listObjects(bucket, s3Prefix).forEach(s3Resource -> {
                try {
                    s3Template.deleteObject(bucket, s3Resource.getFilename());
                } catch (Exception e) {
                    log.error("Failed to delete object during bulk directory clear. key: {}", s3Resource.getFilename(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to list and delete directory from S3. prefix: {}", s3Prefix, e);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED);
        }
    }
}