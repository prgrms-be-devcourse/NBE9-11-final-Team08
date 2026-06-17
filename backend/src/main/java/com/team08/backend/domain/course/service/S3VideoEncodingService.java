package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Profile("prod")
public class S3VideoEncodingService extends VideoEncodingTemplate implements MediaEncodingService {

    private final S3FileStorageService s3FileStorageService;

    public S3VideoEncodingService(LectureDbService lectureDbService, S3FileStorageService s3FileStorageService) {
        super(lectureDbService);
        this.s3FileStorageService = s3FileStorageService;
    }

    // TODO: k6 부하 테스트 진행 후 서버 가용량(CPU/메모리/디스크 I/O) 측정치에 기반하여 videoEncodingExecutor 스레드 풀 제한(max-size, queue-capacity) 설정 반영 예정
    @Override
    @Async("videoEncodingExecutor")
    public void encodeToHls(MultipartFile file, String targetDirName, Long lectureId) {
        executePipeline(file, targetDirName, lectureId);
    }

    @Override
    protected File prepareSourceFile(MultipartFile file, String targetDirName, Long lectureId) {
        String s3SourceKey = "videos/temp/" + targetDirName + ".mp4";
        File tempMultipartFile = null;

        try {
            Path tempMultipartPath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "s3-upload-tmp-", ".mp4");
            tempMultipartFile = tempMultipartPath.toFile();
            file.transferTo(tempMultipartFile);
            s3FileStorageService.uploadFile(tempMultipartFile, s3SourceKey);
        } catch (Exception e) {
            log.error("Failed to upload original video to S3 temp path. lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        } finally {
            if (tempMultipartFile != null && tempMultipartFile.exists()) {
                tempMultipartFile.delete();
            }
        }

        try {
            Path localSourcePath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "s3-source-tmp-", ".mp4");
            File localSourceFile = localSourcePath.toFile();
            s3FileStorageService.downloadFile(s3SourceKey, localSourceFile);
            return localSourceFile;
        } catch (Exception e) {
            log.error("Failed to download original video from S3 temp path. lectureId: {}", lectureId, e);
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
}