package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final LectureDbService lectureDbService;
    private final LectureRepository lectureRepository;
    private final LectureModificationRequestRepository requestRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public LocalVideoEncodingService(LectureDbService lectureDbService,
                                     LectureRepository lectureRepository,
                                     LectureModificationRequestRepository requestRepository) {
        this.lectureDbService = lectureDbService;
        this.lectureRepository = lectureRepository;
        this.requestRepository = requestRepository;
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
        executePipeline(file, targetDirName, lectureId, null);
    }

    @Override
    @Async("videoEncodingExecutor")
    public void encodeModificationToHls(MultipartFile file, String targetDirName, Long lectureId, String description) {
        executePipeline(file, targetDirName, lectureId, description);
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

    @Override
    @Transactional
    public void completePipeline(Long lectureId, String dbSavePath, String targetDirName, String description) {
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