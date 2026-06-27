package com.team08.backend.domain.lecture.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.course.access.CourseAccessAuthorizer;
import com.team08.backend.domain.course.access.CourseAction;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LectureAccessValidatorTest {

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CourseAccessAuthorizer courseAccessAuthorizer;

    @InjectMocks
    private LectureAccessValidator validator;

    private static final Long COURSE_ID = 10L;
    private static final Long CHAPTER_ID = 1L;
    private static final Long LECTURE_ID = 50L;
    private static final Long USER_ID = 7L;

    @Test
    void 비무료_강의는_권한검사를_거쳐_검증된_lecture를_반환한다() {
        Lecture lecture = lecture(false);
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));

        Lecture result = validator.validateForEnter(COURSE_ID, CHAPTER_ID, LECTURE_ID, USER_ID);

        assertThat(result).isSameAs(lecture);
        verify(courseAccessAuthorizer).authorizeByCourseId(COURSE_ID, USER_ID, CourseAction.VIEW_CONTENT);
    }

    @Test
    void 무료_맛보기도_강의를_통한_접근은_권한검사를_거친다() {
        Lecture lecture = lecture(true);
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));

        Lecture result = validator.validateForEnter(COURSE_ID, CHAPTER_ID, LECTURE_ID, USER_ID);

        assertThat(result).isSameAs(lecture);
        // 무료 맛보기 예외가 제거되어 미등록자도 권한 검사를 거친다.
        verify(courseAccessAuthorizer).authorizeByCourseId(COURSE_ID, USER_ID, CourseAction.VIEW_CONTENT);
    }

    @Test
    void 존재하지_않는_강의면_LECTURE_NOT_FOUND() {
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validateForEnter(COURSE_ID, CHAPTER_ID, LECTURE_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    void path_챕터_불일치면_CHAPTER_NOT_FOUND이고_권한검사를_하지_않는다() {
        Lecture lecture = lecture(false);
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));

        assertThatThrownBy(() -> validator.validateForEnter(COURSE_ID, 999L, LECTURE_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);

        verify(courseAccessAuthorizer, never()).authorizeByCourseId(any(), any(), any());
    }

    @Test
    void path_코스_불일치면_COURSE_NOT_FOUND() {
        Lecture lecture = lecture(false);
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));

        assertThatThrownBy(() -> validator.validateForEnter(999L, CHAPTER_ID, LECTURE_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void 권한이_없으면_authorizer의_예외가_전파된다() {
        Lecture lecture = lecture(false);
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));
        willThrow(new CustomException(ErrorCode.COURSE_ACCESS_DENIED))
                .given(courseAccessAuthorizer)
                .authorizeByCourseId(COURSE_ID, USER_ID, CourseAction.VIEW_CONTENT);

        assertThatThrownBy(() -> validator.validateForEnter(COURSE_ID, CHAPTER_ID, LECTURE_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_ACCESS_DENIED);
    }

    @Test
    void validateCourseAccess는_강좌_권한검사를_위임한다() {
        validator.validateCourseAccess(COURSE_ID, USER_ID);

        verify(courseAccessAuthorizer).authorizeByCourseId(COURSE_ID, USER_ID, CourseAction.VIEW_CONTENT);
    }

    @Test
    void validateCourseAccess_권한이_없으면_예외가_전파된다() {
        willThrow(new CustomException(ErrorCode.STUDY_ACCESS_DENIED))
                .given(courseAccessAuthorizer)
                .authorizeByCourseId(COURSE_ID, USER_ID, CourseAction.VIEW_CONTENT);

        assertThatThrownBy(() -> validator.validateCourseAccess(COURSE_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);
    }

    private Lecture lecture(boolean freePreview) {
        Course course = TestEntityFactory.course(COURSE_ID);
        ReflectionTestUtils.setField(course, "id", COURSE_ID);
        Chapter chapter = ChapterFixture.chapter(CHAPTER_ID, "보안 기본", 1, course);
        Lecture lecture = LectureFixture.lecture(LECTURE_ID, "강의1", "videos/1.m3u8", 600, 1, chapter);
        if (freePreview) {
            ReflectionTestUtils.setField(lecture, "isFreePreview", true);
        }
        return lecture;
    }
}
