package com.team08.backend.domain.course.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalVideoEncodingService implements MediaEncodingService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final LectureRepository lectureRepository;

    @Override
    @Async("videoEncodingExecutor")
    @Transactional
    public void encodeToHls(File sourceFile, String targetDirName, Long lectureId) {
        Path targetPath = Paths.get(uploadDir, targetDirName);

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

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }

            String dbSavePath = targetDirName + "/output.m3u8";
            Lecture lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
            lecture.updateM3u8Path(dbSavePath);

        } catch (Exception e) {
            log.error("HLS encoding failed for lectureId: {}", lectureId, e);
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            if (sourceFile.exists()) {
                sourceFile.delete();
            }
        }
    }
}