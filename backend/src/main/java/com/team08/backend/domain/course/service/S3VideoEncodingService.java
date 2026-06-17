package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Profile("prod")
public class S3VideoEncodingService extends VideoEncodingTemplate implements MediaEncodingService {

    private final LectureDbService lectureDbService;
    private final S3FileStorageService s3FileStorageService;
    private final LectureRepository lectureRepository;
    private final LectureModificationRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public S3VideoEncodingService(LectureDbService lectureDbService,
                                  S3FileStorageService s3FileStorageService,
                                  LectureRepository lectureRepository,
                                  LectureModificationRequestRepository requestRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.lectureDbService = lectureDbService;
        this.s3FileStorageService = s3FileStorageService;
        this.lectureRepository = lectureRepository;
        this.requestRepository = requestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeToHls(MultipartFile file, String targetDirName, Long lectureId) {
        executePipeline(file, targetDirName, lectureId, null);
    }

    @Override
    @Async("videoEncodingExecutor")
    @Transactional
    public void encodeModificationToHls(MultipartFile file, String targetDirName, Long lectureId, String description) {
        executePipeline(file, targetDirName, lectureId, description);
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
                if (!tempMultipartFile.delete()) {
                    log.warn("Failed to delete temp multipart file: {}", tempMultipartFile.getAbsolutePath());
                }
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

    @Override
    protected void completePipeline(Long lectureId, String dbSavePath, String targetDirName, String description) {
        if (description == null) {
            lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName);
            return;
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        LectureModificationRequest modificationRequest = LectureModificationRequest.createPending(
                lecture,
                lecture.getChapter().getCourse().getInstructorId(),
                description,
                dbSavePath
        );
        requestRepository.save(modificationRequest);

        eventPublisher.publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
    }

    public void deleteEncodedFolder(String targetDirName, Long lectureId) {
        String s3FolderPath = "lectures/" + lectureId + "/" + targetDirName + "/";
        try {
            s3FileStorageService.deleteDirectory(s3FolderPath);
        } catch (Exception e) {
            log.error("Failed to rollback delete encoded HLS folder in S3. lectureId: {}, folder: {}", lectureId, s3FolderPath, e);
        }
    }
}