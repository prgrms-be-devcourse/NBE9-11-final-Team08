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
import jakarta.servlet.http.HttpServletResponse;
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

    @Mock
    private HttpServletResponse response;

    @Test
    void 무료_미리보기_강의는_권한_검증_없이_스트리밍_주소를_즉시_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findById(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(true);
        given(lecture.getM3u8Path()).willReturn("https://cdn.com/lectures/1/uuid-123/index.m3u8");

        String result = videoAccessService.verifyAndGenerateStreamUrl(lectureId, userId, response);

        assertThat(result).isEqualTo("https://cdn.com/lectures/1/uuid-123/index.m3u8");
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

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamUrl(lectureId, userId, response))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.VIDEO_ACCESS_DENIED.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void 유효한_수강생이면_Signed_Cookie를_헤더에_설정하고_스트리밍_주소를_반환한다() {
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

        given(cloudFrontCookieSigner.createSignedCookies(lectureId, "sample-uuid-path"))
                .willReturn(new ResponseCookie[]{dummyCookie1, dummyCookie2, dummyCookie3});

        String result = videoAccessService.verifyAndGenerateStreamUrl(lectureId, userId, response);

        assertThat(result).isEqualTo(m3u8Path);
        verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
    }
}