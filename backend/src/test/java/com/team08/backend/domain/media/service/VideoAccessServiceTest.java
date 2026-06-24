package com.team08.backend.domain.media.service;

import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.dto.VideoStreamResponse;
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
    private CloudFrontCookieSigner cloudFrontCookieSigner;

    @Mock
    private CourseAccessAuthorizer courseAccessAuthorizer;

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
        verifyNoInteractions(cloudFrontCookieSigner);
        verifyNoInteractions(courseAccessAuthorizer);
    }

    @Test
    void 유효한_수강권이_없는_경우_영상_접근_금지_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;
        String m3u8Path = "https://cdn.com/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";

        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.getM3u8Path()).willReturn(m3u8Path);
        given(lecture.isFreePreview()).willReturn(false);
        doThrow(new CustomException(ErrorCode.COURSE_ACCESS_DENIED))
                .when(courseAccessAuthorizer)
                .authorizeByLectureId(lectureId, userId, CourseAction.VIEW_CONTENT);

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.COURSE_ACCESS_DENIED.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void 유효한_수강생이면_Signed_Cookie_리스트와_경로를_DTO로_묶어_반환한다() {
        Long lectureId = 1L;
        Long userId = 100L;
        String m3u8Path = "https://cdn.com/lectures/1/c0a80101-1234-5678-90ab-cdef12345678/index.m3u8";
        String videoUuid = "c0a80101-1234-5678-90ab-cdef12345678";

        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getM3u8Path()).willReturn(m3u8Path);
        given(lecture.getVideoUuid()).willReturn(videoUuid);

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
    void videoUuid가_null인_경우_잘못된_파라미터_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;

        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(false);
        given(lecture.getVideoUuid()).willReturn(null);

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }

    @Test
    void videoUuid가_공백인_경우_잘못된_파라미터_예외를_던진다() {
        Long lectureId = 1L;
        Long userId = 100L;

        Lecture lecture = mock(Lecture.class);

        given(lectureRepository.findByIdWithChapterAndCourse(lectureId)).willReturn(Optional.of(lecture));
        given(lecture.getVideoUuid()).willReturn("   ");

        assertThatThrownBy(() -> videoAccessService.verifyAndGenerateStreamCookies(lectureId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verifyNoInteractions(cloudFrontCookieSigner);
    }
}