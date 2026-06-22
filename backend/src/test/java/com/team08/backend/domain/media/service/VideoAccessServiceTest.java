package com.team08.backend.domain.media.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
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
    void 무료_미리보기_강의는_권한_검증_없이_빈_쿠키_리스트와_경로를_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        String m3u8Path = "https://cdn.com/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";
        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.getM3u8Path()).willReturn(m3u8Path);
        given(lecture.isFreePreview()).willReturn(true);

        VideoStreamResponse result = videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId);

        assertThat(result.path()).isEqualTo(m3u8Path);
        assertThat(result.cookies()).isEmpty();
        verifyNoInteractions(enrollmentRepository);
        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void 유효한_수강권이_없는_경우_영상_접근_금지_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;
        String m3u8Path = "https://cdn.com/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.getM3u8Path()).willReturn(m3u8Path);
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
    void 유효한_수강생이면_Signed_Cookie_리스트와_경로를_DTO로_묶어_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;
        String m3u8Path = "https://cdn.com/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
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

        given(cloudFrontCookieSigner.createSignedCookies("/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/*", "/lectures/1/"))
                .willReturn(expectedCookies);

        VideoStreamResponse result = videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId);

        assertThat(result.path()).isEqualTo(m3u8Path);
        assertThat(result.cookies()).hasSize(3);
        assertThat(result.cookies()).containsExactly(dummyCookie1, dummyCookie2, dummyCookie3);
    }

    @Test
    void m3u8경로가_null인_경우_잘못된_파라미터_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getChapter()).willReturn(chapter);
        given(chapter.getCourse()).willReturn(course);
        given(course.getId()).willReturn(courseId);
        given(lecture.getM3u8Path()).willReturn(null);

        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void m3u8경로에_유효한_UUID가_없는_경우_잘못된_파라미터_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Long courseId = 50L;
        String invalidM3u8Path = "https://cdn.com/lectures/1/invalid-format-path/index.m3u8";

        Lecture lecture = mock(Lecture.class);
        Chapter chapter = mock(Chapter.class);
        Course course = mock(Course.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getChapter()).willReturn(chapter);
        given(chapter.getCourse()).willReturn(course);
        given(course.getId()).willReturn(courseId);
        given(lecture.getM3u8Path()).willReturn(invalidM3u8Path);

        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }
}