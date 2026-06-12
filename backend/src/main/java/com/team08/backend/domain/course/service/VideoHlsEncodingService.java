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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoHlsEncodingService implements MediaEncodingService {

    private final LectureRepository lectureRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Async("videoEncodingExecutor")
    @Transactional
    public void encodeToHls(File sourceFile, String targetDirName, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        Path targetDirPath = Paths.get(uploadDir, targetDirName);
        try {
            Files.createDirectories(targetDirPath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        }

        String outputFileName = "index.m3u8";
        Path outputFilePath = targetDirPath.resolve(outputFileName);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-i", sourceFile.getAbsolutePath(),
                "-profile:v", "baseline", "-level", "3.0",
                "-start_number", "0", "-hls_time", "10", "-hls_list_size", "0",
                "-f", "hls", outputFilePath.toAbsolutePath().toString()
        );

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.VIDEO_ENCODING_FAILED);
        } finally {
            if (sourceFile.exists()) {
                sourceFile.delete();
            }
        }

        String dbPath = "/" + targetDirName + "/" + outputFileName;
        lecture.updateM3u8Path(dbPath);
    }
}