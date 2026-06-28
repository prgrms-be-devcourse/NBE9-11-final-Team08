package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.EncodingPurpose;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Comparator.reverseOrder;

@Slf4j
@Service
@Profile("!prod")
public class LocalVideoEncodingService extends VideoEncodingTemplate implements MediaEncodingService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public LocalVideoEncodingService(EncodingResultHandler resultHandler) {
        super(resultHandler);
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadDir).toAbsolutePath();
            log.info("=== [VIDEO] upload-dir 설정값: '{}' ===", uploadDir);
            log.info("=== [VIDEO] upload-dir 절대경로: '{}' ===", path);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("=== [VIDEO] upload-dir 폴더 생성 완료 ===");
            }
        } catch (Exception e) {
            log.error("Failed to initialize upload directory: {}", uploadDir, e);
            throw new IllegalStateException("Upload directory initialization failed", e);
        }
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeToHls(File file, String targetDirName, Long lectureId) {
        executePipeline(file, targetDirName, lectureId, EncodingPurpose.CREATE, null, null);
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeModificationToHls(File file, String targetDirName, Long lectureId, String description, Long instructorId) {
        executePipeline(file, targetDirName, lectureId, EncodingPurpose.MODIFY, description, instructorId);
    }

    @Override
    protected File prepareSourceFile(File file, String targetDirName, Long lectureId) {
        return file;
    }

    @Override
    protected void handleGeneratedFiles(Path workspacePath, String targetDirName, Long lectureId) {
        Path targetPath = Paths.get(uploadDir, targetDirName);
        log.info("=== [VIDEO] 복사 시작 - 원본: '{}', 대상: '{}' (절대경로: '{}') ===", 
                workspacePath, targetPath, targetPath.toAbsolutePath());
        try {
            Files.createDirectories(targetPath);
            log.info("=== [VIDEO] 대상 디렉터리 생성 완료: '{}' ===", targetPath.toAbsolutePath());
            try (var stream = Files.walk(workspacePath)) {
                stream.forEach(path -> {
                    try {
                        if (Files.isRegularFile(path)) {
                            Path targetFile = targetPath.resolve(workspacePath.relativize(path));
                            Files.createDirectories(targetFile.getParent());
                            Files.copy(path, targetFile);
                            log.info("=== [VIDEO] 파일 복사 완료: '{}' -> '{}' ===", path.getFileName(), targetFile.toAbsolutePath());
                        }
                    } catch (IOException e) {
                        log.error("=== [VIDEO] 파일 복사 실패: '{}', 에러: {} ===", path, e.getMessage());
                        throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
                    }
                });
            }
            log.info("=== [VIDEO] 모든 파일 복사 완료! lectureId: {} ===", lectureId);
        } catch (Exception e) {
            log.error("=== [VIDEO] HLS 파일 복사 실패! lectureId: {}, 에러: {} ===", lectureId, e.getMessage(), e);
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

    @Override
    public void deleteEncodedFolder(String targetDirName, Long lectureId) {
        Path targetPath = Paths.get(uploadDir, targetDirName);
        if (Files.exists(targetPath)) {
            try (var stream = Files.walk(targetPath)) {
                stream.sorted(reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete path: " + path, e);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to rollback delete encoded HLS directory. folder: {}", targetPath, e);
                throw new RuntimeException("Failed to delete directory: " + targetPath, e);
            }
        }
    }
}