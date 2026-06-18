package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.auth.util.CloudFrontCookieSigner;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoAccessServiceTest {

    @InjectMocks
    private VideoAccessService videoAccessService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CloudFrontCookieSigner cloudFrontCookieSigner;

    @Test
    void 무료_미리보기_강의는_권한_검증_없이_빈_쿠키_배열을_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(true);

        ResponseCookie[] result = videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId);

        assertThat(result).isEmpty();
        verifyNoInteractions(enrollmentRepository);
        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void 유효한_수강권이_없는_경우_영상_접근_금지_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getChapter()).willReturn(chapter);
        given(chapter.getCourse()).willReturn(course);
        given(course.getId()).willReturn(courseId);

        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
                .willReturn(false);

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.VIDEO_ACCESS_DENIED.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void 유효한_수강생이면_Signed_Cookie_배열을_생성하여_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;
        String m3u8Path = "https://cdn.com/lectures/1/sample-uuid-path/index.m3u8";

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getChapter()).willReturn(chapter);
        given(chapter.getCourse()).willReturn(course);
        given(course.getId()).willReturn(courseId);
        given(lecture.getM3u8Path()).willReturn(m3u8Path);

        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
                .willReturn(true);

        ResponseCookie dummyCookie1 = ResponseCookie.from("CloudFront-Policy", "policy").build();
        ResponseCookie dummyCookie2 = ResponseCookie.from("CloudFront-Signature", "sig").build();
        ResponseCookie dummyCookie3 = ResponseCookie.from("CloudFront-Key-Pair-Id", "key").build();
        ResponseCookie[] expectedCookies = new ResponseCookie[]{dummyCookie1, dummyCookie2, dummyCookie3};

        given(cloudFrontCookieSigner.createSignedCookies(lectureId, "sample-uuid-path"))
                .willReturn(expectedCookies);

        ResponseCookie[] result = videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(dummyCookie1, dummyCookie2, dummyCookie3);
    }

    @Test
    void 강의_재생_경로를_정상적으로_조회한다() {
        Long lectureId = 1L;
        String m3u8Path = "https://cdn.com/lectures/1/sample-uuid-path/index.m3u8";
        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.getM3u8Path()).willReturn(m3u8Path);

        String result = videoAccessService.getPlayableM3u8Path(lectureId);

        assertThat(result).isEqualTo(m3u8Path);
    }
}