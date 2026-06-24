package com.team08.backend.domain.lecturereflection.service;

import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturereflection.dto.LectureReflectionResponse;
import com.team08.backend.domain.lecturereflection.entity.LectureReflection;
import com.team08.backend.domain.lecturereflection.repository.LectureReflectionRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.team08.backend.domain.lecturereflection.fixture.LectureReflectionFixture.reflection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LectureReflectionServiceTest {

    @Mock
    private LectureReflectionRepository reflectionRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CourseAccessAuthorizer courseAccessAuthorizer;

    @InjectMocks
    private LectureReflectionService reflectionService;

    @Test
    @DisplayName("회고 작성 성공")
    void createReflection_success() {
        Long userId = 1L;
        Long lectureId = 10L;

        Lecture lecture = mock(Lecture.class);
        given(lecture.getDeletedAt()).willReturn(null);

        given(lectureRepository.findById(lectureId))
                .willReturn(Optional.of(lecture));

        given(reflectionRepository.existsByUserIdAndLectureId(userId, lectureId))
                .willReturn(false);

        given(reflectionRepository.save(any()))
                .willReturn(reflection(userId, lectureId, "회고 내용"));

        LectureReflectionResponse response =
                reflectionService.createReflection(userId, lectureId, "회고 내용");

        assertThat(response.content()).isEqualTo("회고 내용");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.lectureId()).isEqualTo(lectureId);
        verify(courseAccessAuthorizer).authorizeByLectureId(lectureId, userId, CourseAction.WRITE_CONTENT);
    }

    @Test
    @DisplayName("회고 작성 실패 - 강의 없음")
    void createReflection_lectureNotFound() {
        given(lectureRepository.findById(any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                reflectionService.createReflection(1L, 10L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("회고 작성 실패 - 이미 회고 존재")
    void createReflection_alreadyExists() {
        Long userId = 1L;
        Long lectureId = 10L;

        Lecture lecture = mock(Lecture.class);
        given(lecture.getDeletedAt()).willReturn(null);

        given(lectureRepository.findById(lectureId))
                .willReturn(Optional.of(lecture));

        given(reflectionRepository.existsByUserIdAndLectureId(userId, lectureId))
                .willReturn(true);

        assertThatThrownBy(() ->
                reflectionService.createReflection(userId, lectureId, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFLECTION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("회고 수정 성공")
    void updateReflection_success() {
        Long userId = 1L;
        Long reflectionId = 100L;

        LectureReflection reflection =
                reflection(userId, 10L, "old content");

        given(reflectionRepository.findById(reflectionId))
                .willReturn(Optional.of(reflection));

        LectureReflectionResponse response =
                reflectionService.updateReflection(
                        reflectionId,
                        userId,
                        "new content"
                );

        assertThat(response.content()).isEqualTo("new content");
    }

    @Test
    @DisplayName("회고 수정 실패 - 회고 없음")
    void updateReflection_notFound() {
        given(reflectionRepository.findById(any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                reflectionService.updateReflection(100L, 1L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETROSPECTION_NOT_FOUND);
    }

    @Test
    @DisplayName("회고 수정 실패 - 작성자가 아님")
    void updateReflection_accessDenied() {
        Long ownerId = 1L;
        Long otherId = 2L;

        LectureReflection reflection =
                reflection(ownerId, 10L, "content");

        given(reflectionRepository.findById(any()))
                .willReturn(Optional.of(reflection));

        assertThatThrownBy(() ->
                reflectionService.updateReflection(100L, otherId, "content"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFLECTION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("회고 조회 성공")
    void getReflection_success() {
        Long userId = 1L;
        Long lectureId = 10L;

        LectureReflection reflection =
                reflection(userId, lectureId, "회고 내용");

        given(reflectionRepository.findByUserIdAndLectureId(userId, lectureId))
                .willReturn(Optional.of(reflection));

        LectureReflectionResponse response =
                reflectionService.getReflection(userId, lectureId);

        assertThat(response.content()).isEqualTo("회고 내용");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.lectureId()).isEqualTo(lectureId);
    }

    // TODO:여기 프론트연동하다 에러발견
//    @Test
//    @DisplayName("회고 조회 실패 - 회고 없음")
//    void getReflection_notFound() {
//        given(reflectionRepository.findByUserIdAndLectureId(any(), any()))
//                .willReturn(Optional.empty());
//
//        assertThatThrownBy(() ->
//                reflectionService.getReflection(1L, 10L))
//                .isInstanceOf(CustomException.class)
//                .extracting(e -> ((CustomException) e).getErrorCode())
//                .isEqualTo(ErrorCode.RETROSPECTION_NOT_FOUND);
//    }
}