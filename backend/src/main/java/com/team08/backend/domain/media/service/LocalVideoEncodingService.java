package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.EncodingPurpose;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Comparator.reverseOrder;

@Slf4j
@Service
public class LocalVideoEncodingService extends VideoEncodingTemplate implements MediaEncodingService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public LocalVideoEncodingService(EncodingResultHandler resultHandler) {
        super(resultHandler);
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("Failed to initialize upload directory: {}", uploadDir, e);
            throw new IllegalStateException("Upload directory initialization failed", e);
        }
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeToHls(MultipartFile file, String targetDirName, Long lectureId) {
        executePipeline(file, targetDirName, lectureId, EncodingPurpose.CREATE, null, null);
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeModificationToHls(MultipartFile file, String targetDirName, Long lectureId, String description, Long instructorId) {
        executePipeline(file, targetDirName, lectureId, EncodingPurpose.MODIFY, description, instructorId);
    }

    @Override
    protected File prepareSourceFile(MultipartFile file, String targetDirName, Long lectureId) {
        try {
            Path tempFilePath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "lecture-local-tmp-", ".mp4");
            File sourceFile = tempFilePath.toFile();
            file.transferTo(sourceFile);
            return sourceFile;
        } catch (Exception e) {
            log.error("Failed to create temporary file for local encoding. lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        }
    }

    @Override
    protected void handleGeneratedFiles(Path workspacePath, String targetDirName, Long lectureId) {
        Path targetPath = Paths.get(uploadDir, targetDirName);
        try {
            Files.createDirectories(targetPath);
            try (var stream = Files.walk(workspacePath)) {
                stream.forEach(path -> {
                    try {
                        if (Files.isRegularFile(path)) {
                            Path targetFile = targetPath.resolve(workspacePath.relativize(path));
                            Files.createDirectories(targetFile.getParent());
                            Files.copy(path, targetFile);
                        }
                    } catch (IOException e) {
                        throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to copy generated HLS files to local upload directory. lectureId: {}", lectureId, e);
            if (Files.exists(targetPath)) {
                try (var stream = Files.walk(targetPath)) {
                    stream.sorted(reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ignored) {}
                            });
                } catch (Exception ex) {
                    log.error("Failed to clean up target directory: {}", targetPath, ex);
                }
            }
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        }
    }

    @Override
    protected String getDbSavePath(String targetDirName, Long lectureId) {
        return targetDirName + "/output.m3u8";
    }

    public void deleteEncodedFolder(String targetDirName) {
        Path targetPath = Paths.get(uploadDir, targetDirName);
        if (Files.exists(targetPath)) {
            try (var stream = Files.walk(targetPath)) {
                stream.sorted(reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {}
                        });
            } catch (Exception e) {
                log.error("Failed to rollback delete encoded HLS directory. folder: {}", targetPath, e);
            }
        }
    }
}