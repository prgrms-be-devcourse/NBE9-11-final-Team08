package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.auth.util.CloudFrontCookieSigner;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VideoAccessService {

    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CloudFrontCookieSigner cloudFrontCookieSigner;
    private static final Pattern UUID_PATTERN = Pattern.compile("/lectures/\\d+/([^/]+)/");

    @Transactional(readOnly = true)
    public ResponseCookie[] verifyAndGenerateStreamCookies(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        if (lecture.isFreePreview()) {
            return new ResponseCookie[0];
        }

        Long courseId = lecture.getChapter().getCourse().getId();
        boolean hasValidEnrollment = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE
        );

        if (!hasValidEnrollment) {
            throw new CustomException(ErrorCode.VIDEO_ACCESS_DENIED);
        }

        String uuid = extractUuid(lecture.getM3u8Path());
        return cloudFrontCookieSigner.createSignedCookies(lectureId, uuid);
    }

    @Transactional(readOnly = true)
    public String getPlayableM3u8Path(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        return lecture.getM3u8Path();
    }

    private String extractUuid(String m3u8Path) {
        Matcher matcher = UUID_PATTERN.matcher(m3u8Path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}