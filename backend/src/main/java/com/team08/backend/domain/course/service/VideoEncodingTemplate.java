package com.team08.backend.domain.course.service;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.util.Comparator.reverseOrder;

@Slf4j
@RequiredArgsConstructor
public abstract class VideoEncodingTemplate {

    protected void executePipeline(MultipartFile file, String targetDirName, Long lectureId, String description, Long instructorId) {
        File sourceFile = prepareSourceFile(file, targetDirName, lectureId);
        Process process = null;
        Path localWorkspacePath = null;

        try {
            localWorkspacePath = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "hls-work-");

            String outputM3u8 = localWorkspacePath.resolve("output.m3u8").toString();
            String segmentPattern = localWorkspacePath.resolve("segment_%03d.ts").toString();

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

            handleGeneratedFiles(localWorkspacePath, targetDirName, lectureId);

            String dbSavePath = getDbSavePath(targetDirName, lectureId);
            completePipeline(lectureId, dbSavePath, targetDirName, description, instructorId);

        } catch (Exception e) {
            log.error("HLS Processing Pipeline Exception for lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (sourceFile != null && sourceFile.exists()) {
                sourceFile.delete();
            }
            if (localWorkspacePath != null) {
                try (var stream = Files.walk(localWorkspacePath)) {
                    stream.sorted(reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.warn("Cleanup failed for temporary workspace path: {}", path, e);
                                }
                            });
                } catch (Exception e) {
                    log.error("Failed to clean up temporary directory: {}", localWorkspacePath, e);
                }
            }
        }
    }

    protected abstract File prepareSourceFile(MultipartFile file, String targetDirName, Long lectureId);

    protected abstract void handleGeneratedFiles(Path workspacePath, String targetDirName, Long lectureId);

    protected abstract String getDbSavePath(String targetDirName, Long lectureId);

    protected abstract void completePipeline(Long lectureId, String dbSavePath, String targetDirName, String description, Long instructorId);
}