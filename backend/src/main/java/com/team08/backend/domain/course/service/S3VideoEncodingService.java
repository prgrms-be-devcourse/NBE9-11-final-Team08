package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.S3FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3VideoEncodingService implements MediaEncodingService {

    private final LectureRepository lectureRepository;
    private final S3FileStorageService s3FileStorageService;

    @Override
    @Async("videoEncodingExecutor")
    @Transactional
    public void encodeToHls(MultipartFile file, String targetDirName, Long lectureId) {
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

        File localSourceFile = null;
        Path localWorkspacePath = null;
        Process process = null;

        try {
            Path localSourcePath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "s3-source-tmp-", ".mp4");
            localSourceFile = localSourcePath.toFile();
            s3FileStorageService.downloadFile(s3SourceKey, localSourceFile);

            localWorkspacePath = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "hls-work-");

            String outputM3u8 = localWorkspacePath.resolve("output.m3u8").toString();
            String segmentPattern = localWorkspacePath.resolve("segment_%03d.ts").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", localSourceFile.getAbsolutePath(),
                    "-profile:v", "baseline", "-level", "3.0",
                    "-s", "1280x720", "-start_number", "0",
                    "-hls_time", "10", "-hls_list_size", "0",
                    "-f", "hls", "-hls_segment_filename", segmentPattern,
                    outputM3u8
            );

            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("S3 HLS encoding timeout exceeded for lectureId: {}", lectureId);
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("S3 HLS encoding failed. exitCode: {}, lectureId: {}", exitCode, lectureId);
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            File[] generatedFiles = localWorkspacePath.toFile().listFiles();
            if (generatedFiles != null) {
                for (File f : generatedFiles) {
                    String s3TargetKey = "lectures/" + lectureId + "/" + targetDirName + "/" + f.getName();
                    s3FileStorageService.uploadFile(f, s3TargetKey);
                }
            }

            String dbSavePath = "lectures/" + lectureId + "/" + targetDirName + "/output.m3u8";
            Lecture lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
            lecture.updateM3u8Path(dbSavePath);

        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            log.error("S3 HLS Processing Pipeline Exception for lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            s3FileStorageService.deleteFile(s3SourceKey);
            if (localSourceFile != null && localSourceFile.exists()) {
                localSourceFile.delete();
            }
            if (localWorkspacePath != null) {
                try (var stream = Files.walk(localWorkspacePath)) {
                    stream.map(Path::toFile).forEach(File::delete);
                } catch (Exception ignored) {}
            }
        }
    }
}