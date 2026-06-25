package com.team08.backend.domain.course.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.team08.backend.domain.user.dto.LoginUserDto;
import com.team08.backend.global.auth.principal.LoginUserPrincipal;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CourseAccessContextResolverTest {

    private static final Long COURSE_ID = 10L;
    private static final Long CHAPTER_ID = 20L;
    private static final Long LECTURE_ID = 30L;
    private static final Long USER_ID = 100L;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseAccessContextResolver resolver;

    @BeforeEach
    void setUp() {
        User defaultUser = User.createUser("user@test.com", "password", "유저", null);
        ReflectionTestUtils.setField(defaultUser, "id", USER_ID);
        org.mockito.Mockito.lenient().when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(defaultUser));
    }

    @Test
    void chapterId로_course를_찾아_권한_context를_생성한다() {
        Chapter chapter = chapter();
        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.of(chapter));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        CourseAccessContext context = resolver.fromChapterId(CHAPTER_ID, USER_ID);

        assertContext(context, CourseStatus.ON_SALE, false, true, false, false);
    }

    @Test
    void lectureId로_course를_찾아_권한_context를_생성한다() {
        Lecture lecture = lecture();
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID))
                .willReturn(Optional.of(lecture));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        CourseAccessContext context = resolver.fromLectureId(LECTURE_ID, USER_ID);

        assertContext(context, CourseStatus.ON_SALE, false, true, false, false);
    }

    @Test
    void 어드민인_경우_isAdmin이_true인_context를_생성한다() {
        Chapter chapter = chapter();
        User adminUser = User.createAdmin("admin@test.com", "password", "어드민", null);
        ReflectionTestUtils.setField(adminUser, "id", USER_ID);

        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.of(chapter));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));

        CourseAccessContext context = resolver.fromChapterId(CHAPTER_ID, USER_ID);

        assertContext(context, CourseStatus.ON_SALE, false, true, false, true);
    }

    @Test
    void 비로그인인_경우_isAdmin이_false인_context를_생성한다() {
        Chapter chapter = chapter();
        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.of(chapter));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(null, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        CourseAccessContext context = resolver.fromChapterId(CHAPTER_ID, null);

        assertThat(context.userId()).isNull();
        assertThat(context.isAdmin()).isFalse();
    }

    @Test
    void 챕터가_없으면_CHAPTER_NOT_FOUND를_던진다() {
        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.empty());

        assertErrorCode(
                () -> resolver.fromChapterId(CHAPTER_ID, USER_ID),
                ErrorCode.CHAPTER_NOT_FOUND
        );
    }

    @Test
    void 강의가_없으면_LECTURE_NOT_FOUND를_던진다() {
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID))
                .willReturn(Optional.empty());

        assertErrorCode(
                () -> resolver.fromLectureId(LECTURE_ID, USER_ID),
                ErrorCode.LECTURE_NOT_FOUND
        );
    }

    @Test
    void SecurityContext가_존재하고_userId가_일치하면_DB_조회없이_isAdmin을_설정한다() {
        Chapter chapter = chapter();
        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.of(chapter));
        given(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).willReturn(false);

        LoginUserDto loginUserDto = new LoginUserDto(USER_ID, "admin@test.com", "관리자", "ROLE_ADMIN");
        LoginUserPrincipal principal = LoginUserPrincipal.from(loginUserDto);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            CourseAccessContext resultContext = resolver.fromChapterId(CHAPTER_ID, USER_ID);

            assertContext(resultContext, CourseStatus.ON_SALE, false, true, false, true);
            org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).findById(USER_ID);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Chapter chapter() {
        Course course = TestEntityFactory.course(COURSE_ID);
        ReflectionTestUtils.setField(course, "instructorId", USER_ID);
        ReflectionTestUtils.setField(course, "status", CourseStatus.ON_SALE);

        return ChapterFixture.chapter(CHAPTER_ID, "챕터 제목", 1, course);
    }

    private Lecture lecture() {
        return LectureFixture.lecture(
                LECTURE_ID,
                "강의 제목",
                "lecture.m3u8",
                600,
                1,
                chapter()
        );
    }

    private void assertContext(
            CourseAccessContext context,
            CourseStatus courseStatus,
            boolean hasActiveEnrollment,
            boolean isOwner,
            boolean hasFreePreview,
            boolean isAdmin
    ) {
        assertThat(context.userId()).isEqualTo(USER_ID);
        assertThat(context.courseStatus()).isEqualTo(courseStatus);
        assertThat(context.hasActiveEnrollment()).isEqualTo(hasActiveEnrollment);
        assertThat(context.isOwner()).isEqualTo(isOwner);
        assertThat(context.hasFreePreview()).isEqualTo(hasFreePreview);
        assertThat(context.isAdmin()).isEqualTo(isAdmin);
    }

    private void assertErrorCode(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(errorCode);
    }
}
