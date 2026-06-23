package com.team08.backend.domain.media.service;

import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAccessService {

    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CloudFrontCookieSigner cloudFrontCookieSigner;

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

        String videoUuid = lecture.getVideoUuid();
        if (videoUuid == null || videoUuid.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String resourcePath = "/lectures/" + lectureId + "/" + videoUuid + "/*";
        String cookiePath = "/lectures/" + lectureId + "/";
        ResponseCookie[] cookies = cloudFrontCookieSigner.createSignedCookies(resourcePath, cookiePath);

        return new VideoStreamResponse(m3u8Path, Arrays.asList(cookies));
    }
}