package com.team08.backend.domain.study.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.fixture.ChapterFixture;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.fixture.LectureFixture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.user.entity.User;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudyAccessContextResolverTest {

    private static final Long STUDY_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long CHAPTER_ID = 20L;
    private static final Long LECTURE_ID = 30L;
    private static final Long USER_ID = 100L;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private EnrollmentQueryService enrollmentQueryService;

    @InjectMocks
    private StudyAccessContextResolver resolver;

    @Test
    void studyId로_권한_context를_생성한다() {
        Study study = study(StudyStatus.ACTIVE);
        StudyMember member = studyMember(study, StudyMemberRole.MEMBER);
        given(studyRepository.findById(STUDY_ID)).willReturn(Optional.of(study));
        givenActiveMember(member);
        given(enrollmentQueryService.hasActiveEnrollment(USER_ID, COURSE_ID)).willReturn(true);

        StudyAccessContext context = resolver.fromStudyId(STUDY_ID, USER_ID);

        assertContext(context, StudyStatus.ACTIVE, true, true, StudyMemberRole.MEMBER);
    }

    @Test
    void courseId로_권한_context를_생성한다() {
        Study study = study(StudyStatus.ACTIVE);
        StudyMember member = studyMember(study, StudyMemberRole.OWNER);
        given(studyRepository.findByCourseIdAndStatusNot(COURSE_ID, StudyStatus.INACTIVE))
                .willReturn(Optional.of(study));
        givenActiveMember(member);
        given(enrollmentQueryService.hasActiveEnrollment(USER_ID, COURSE_ID)).willReturn(true);

        StudyAccessContext context = resolver.fromCourseId(COURSE_ID, USER_ID);

        assertContext(context, StudyStatus.ACTIVE, true, true, StudyMemberRole.OWNER);
    }

    @Test
    void chapterId로_course를_찾아_권한_context를_생성한다() {
        Study study = study(StudyStatus.READONLY);
        Chapter chapter = chapter();
        given(chapterRepository.findByIdWithCourse(CHAPTER_ID)).willReturn(Optional.of(chapter));
        given(studyRepository.findByCourseIdAndStatusNot(COURSE_ID, StudyStatus.INACTIVE))
                .willReturn(Optional.of(study));
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                STUDY_ID,
                USER_ID,
                StudyMemberStatus.ACTIVE
        )).willReturn(Optional.empty());
        given(enrollmentQueryService.hasActiveEnrollment(USER_ID, COURSE_ID)).willReturn(false);

        StudyAccessContext context = resolver.fromChapterId(CHAPTER_ID, USER_ID);

        assertContext(context, StudyStatus.READONLY, false, false, null);
    }

    @Test
    void lectureId로_course를_찾아_권한_context를_생성한다() {
        Study study = study(StudyStatus.DRAFT);
        StudyMember owner = studyMember(study, StudyMemberRole.OWNER);
        Lecture lecture = lecture();
        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID))
                .willReturn(Optional.of(lecture));
        given(studyRepository.findByCourseIdAndStatusNot(COURSE_ID, StudyStatus.INACTIVE))
                .willReturn(Optional.of(study));
        givenActiveMember(owner);
        given(enrollmentQueryService.hasActiveEnrollment(USER_ID, COURSE_ID)).willReturn(false);

        StudyAccessContext context = resolver.fromLectureId(LECTURE_ID, USER_ID);

        assertContext(context, StudyStatus.DRAFT, false, true, StudyMemberRole.OWNER);
    }

    @Test
    void 스터디가_없으면_STUDY_NOT_FOUND를_던진다() {
        given(studyRepository.findById(STUDY_ID)).willReturn(Optional.empty());

        assertErrorCode(
                () -> resolver.fromStudyId(STUDY_ID, USER_ID),
                ErrorCode.STUDY_NOT_FOUND
        );
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

    private Study study(StudyStatus status) {
        User owner = TestEntityFactory.user(USER_ID);
        Course course = TestEntityFactory.course(COURSE_ID);
        Study study = Study.createForCourse(owner, course, "스터디 제목", "스터디 설명");
        ReflectionTestUtils.setField(study, "id", STUDY_ID);
        ReflectionTestUtils.setField(study, "status", status);
        return study;
    }

    private Chapter chapter() {
        Course course = TestEntityFactory.course(COURSE_ID);
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

    private StudyMember studyMember(Study study, StudyMemberRole role) {
        StudyMember member = StudyMember.owner(TestEntityFactory.user(USER_ID), study);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }

    private void givenActiveMember(StudyMember member) {
        given(studyMemberRepository.findByStudyIdAndUserIdAndStatus(
                STUDY_ID,
                USER_ID,
                StudyMemberStatus.ACTIVE
        )).willReturn(Optional.of(member));
    }

    private void assertContext(
            StudyAccessContext context,
            StudyStatus studyStatus,
            boolean hasActiveEnrollment,
            boolean activeMember,
            StudyMemberRole memberRole
    ) {
        assertThat(context.studyId()).isEqualTo(STUDY_ID);
        assertThat(context.courseId()).isEqualTo(COURSE_ID);
        assertThat(context.userId()).isEqualTo(USER_ID);
        assertThat(context.studyStatus()).isEqualTo(studyStatus);
        assertThat(context.hasActiveEnrollment()).isEqualTo(hasActiveEnrollment);
        assertThat(context.activeMember()).isEqualTo(activeMember);
        assertThat(context.memberRole()).isEqualTo(memberRole);
    }

    private void assertErrorCode(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(errorCode);
    }
}
