package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.TimeUnit;

import static java.util.Comparator.reverseOrder;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalVideoEncodingService implements MediaEncodingService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final LectureDbService lectureDbService;

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

    // TODO: k6 부하 테스트 진행 후 서버 가용량(CPU/메모리/디스크 I/O) 측정치에 기반하여 videoEncodingExecutor 스레드 풀 제한(max-size, queue-capacity) 설정 반영 예정
    @Override
    @Async("videoEncodingExecutor")
    public void encodeToHls(MultipartFile file, String targetDirName, Long lectureId) {
        Path targetPath = Paths.get(uploadDir, targetDirName);
        File sourceFile;

        try {
            Path tempFilePath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "lecture-local-tmp-", ".mp4");
            sourceFile = tempFilePath.toFile();
            file.transferTo(sourceFile);
        } catch (Exception e) {
            log.error("Failed to create temporary file for local encoding. lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_UPLOAD_FAILED);
        }

        Process process = null;
        boolean isSuccessful = false;

        try {
            Files.createDirectories(targetPath);

            String outputM3u8 = targetPath.resolve("output.m3u8").toString();
            String segmentPattern = targetPath.resolve("segment_%03d.ts").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", sourceFile.getAbsolutePath(),
                    "-profile:v", "baseline", "-level", "3.0",
                    "-s", "1280x720", "-start_number", "0",
                    "-hls_time", "10", "-hls_list_size", "0",
                    "-f", "hls", "-hls_segment_filename", segmentPattern,
                    outputM3u8
            );

            pb.inheritIO();
            process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                log.error("HLS encoding timeout exceeded for lectureId: {}", lectureId);
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("HLS encoding failed. exitCode: {}, lectureId: {}", exitCode, lectureId);
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            File[] generatedFiles = targetPath.toFile().listFiles();
            if (generatedFiles == null || generatedFiles.length == 0) {
                log.error("HLS encoding generated zero files. lectureId: {}", lectureId);
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            String dbSavePath = targetDirName + "/output.m3u8";
            lectureDbService.updateLectureM3u8(lectureId, dbSavePath);

            isSuccessful = true;

        } catch (Exception e) {
            log.error("HLS encoding failed for lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (sourceFile != null && sourceFile.exists()) {
                sourceFile.delete();
            }
            if (!isSuccessful && targetPath != null && Files.exists(targetPath)) {
                try (var stream = Files.walk(targetPath)) {
                    stream.sorted(reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.warn("Cleanup failed for incomplete target path: {}", path, e);
                                }
                            });
                } catch (Exception e) {
                    log.error("Failed to clean up incomplete target directory: {}", targetPath, e);
                }
            }
        }
    }
}