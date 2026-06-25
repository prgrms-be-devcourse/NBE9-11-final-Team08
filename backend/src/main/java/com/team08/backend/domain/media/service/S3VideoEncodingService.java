package com.team08.backend.domain.media.service;

import com.team08.backend.domain.media.entity.EncodingPurpose;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Profile("prod")
public class S3VideoEncodingService extends VideoEncodingTemplate implements MediaEncodingService {

    private final S3FileStorageService s3FileStorageService;

    public S3VideoEncodingService(EncodingResultHandler resultHandler,
                                  S3FileStorageService s3FileStorageService) {
        super(resultHandler);
        this.s3FileStorageService = s3FileStorageService;
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
        String s3SourceKey = "videos/temp/" + targetDirName + ".mp4";

        try {
            s3FileStorageService.uploadFile(file, s3SourceKey);
        } catch (Exception e) {
            log.error("Failed to upload original video to S3 temp path. lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        } finally {
            if (file != null && file.exists()) {
                if (!file.delete()) {
                    log.warn("Failed to delete temp multipart file: {}", file.getAbsolutePath());
                }
            }
        }

        File localSourceFile = null;
        try {
            Path localSourcePath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "s3-source-tmp-", ".mp4");
            localSourceFile = localSourcePath.toFile();
            s3FileStorageService.downloadFile(s3SourceKey, localSourceFile);
            return localSourceFile;
        } catch (Exception e) {
            log.error("Failed to download original video from S3 temp path. lectureId: {}", lectureId, e);
            if (localSourceFile != null && localSourceFile.exists()) {
                localSourceFile.delete();
            }
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            s3FileStorageService.deleteFile(s3SourceKey);
        }
    }

    @Override
    protected void handleGeneratedFiles(Path workspacePath, String targetDirName, Long lectureId) {
        File[] generatedFiles = workspacePath.toFile().listFiles();
        if (generatedFiles == null || generatedFiles.length == 0) {
            log.error("S3 HLS encoding generated zero files. lectureId: {}", lectureId);
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        }

        for (File f : generatedFiles) {
            String s3TargetKey = "lectures/" + lectureId + "/" + targetDirName + "/" + f.getName();
            s3FileStorageService.uploadFile(f, s3TargetKey);
        }
    }

    @Override
    protected String getDbSavePath(String targetDirName, Long lectureId) {
        return "lectures/" + lectureId + "/" + targetDirName + "/output.m3u8";
    }

    @Override
    public void deleteEncodedFolder(String targetDirName, Long lectureId) {
        String s3FolderPath = "lectures/" + lectureId + "/" + targetDirName + "/";
        try {
            s3FileStorageService.deleteDirectory(s3FolderPath);
        } catch (Exception e) {
            log.error("Failed to rollback delete encoded HLS folder in S3. lectureId: {}, folder: {}", lectureId, s3FolderPath, e);
        }
    }
}