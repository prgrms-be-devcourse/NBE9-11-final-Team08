package com.team08.backend.domain.media.service;

import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
import com.team08.backend.global.auth.util.CloudFrontCookieSigner;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.global.util.FileUrlFormatter;
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
    private final CloudFrontCookieSigner cloudFrontCookieSigner;
    private final CourseAccessAuthorizer courseAccessAuthorizer;
    private final FileUrlFormatter fileUrlFormatter;

    @Transactional(readOnly = true)
    public VideoStreamResponse verifyAndGenerateStreamCookies(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        String m3u8Path = fileUrlFormatter.formatVideoUrl(lecture.getM3u8Path());

        if (!lecture.isFreePreview()) {
            // TODO: 권한 추가. 도훈님 확인 필요
            courseAccessAuthorizer.authorizeByLectureId(lectureId, userId, CourseAction.VIEW_CONTENT);
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