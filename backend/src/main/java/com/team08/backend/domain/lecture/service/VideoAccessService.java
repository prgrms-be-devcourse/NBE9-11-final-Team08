package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.dto.VideoStreamResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.auth.util.CloudFrontCookieSigner;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAccessService {

    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CloudFrontCookieSigner cloudFrontCookieSigner;
    private static final Pattern LECTURE_PATH_PATTERN = Pattern.compile("/lectures/\\d+/([a-f0-9\\-]{36})(?:/|$)");

    @Transactional(readOnly = true)
    public VideoStreamResponse verifyAndGenerateStreamCookies(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        String m3u8Path = lecture.getM3u8Path();

        if (lecture.isFreePreview()) {
            return new VideoStreamResponse(m3u8Path, List.of());
        }

        Long courseId = lecture.getChapter().getCourse().getId();
        boolean hasValidEnrollment = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE
        );

        if (!hasValidEnrollment) {
            throw new CustomException(ErrorCode.VIDEO_ACCESS_DENIED);
        }

        String uuid = extractUuid(m3u8Path);
        String resourcePath = "/lectures/" + lectureId + "/" + uuid + "/*";
        String cookiePath = "/lectures/" + lectureId + "/";
        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(resourcePath, cookiePath);

        return new VideoStreamResponse(m3u8Path, Arrays.asList(cookies));
    }

    private String extractUuid(String m3u8Path) {
        if (m3u8Path == null) {
            log.warn("Lecture path data integrity violation: path is null");
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Matcher matcher = LECTURE_PATH_PATTERN.matcher(m3u8Path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("Lecture path format mismatch validation failed. Input: [{}], Expected Pattern: [{}]", m3u8Path, LECTURE_PATH_PATTERN.pattern());
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}