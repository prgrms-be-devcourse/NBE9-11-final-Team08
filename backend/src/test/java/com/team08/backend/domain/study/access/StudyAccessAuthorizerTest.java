package com.team08.backend.domain.study.access;

import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyAccessAuthorizerTest {

    private static final Long STUDY_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long CHAPTER_ID = 20L;
    private static final Long LECTURE_ID = 30L;
    private static final Long USER_ID = 100L;

    @Mock
    private StudyAccessContextResolver contextResolver;

    @Mock
    private StudyAccessPolicy policy;

    @InjectMocks
    private StudyAccessAuthorizer authorizer;

    @Test
    void studyId로_context를_만들고_권한을_검사한다() {
        StudyAccessContext context = context();
        given(contextResolver.fromStudyId(STUDY_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByStudyId(STUDY_ID, USER_ID, StudyAction.VIEW_STUDY_CONTENT);

        verify(policy).authorize(context, StudyAction.VIEW_STUDY_CONTENT);
    }

    @Test
    void courseId로_context를_만들고_권한을_검사한다() {
        StudyAccessContext context = context();
        given(contextResolver.fromCourseId(COURSE_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByCourseId(COURSE_ID, USER_ID, StudyAction.MANAGE_CURRICULUM);

        verify(policy).authorize(context, StudyAction.MANAGE_CURRICULUM);
    }

    @Test
    void chapterId로_context를_만들고_권한을_검사한다() {
        StudyAccessContext context = context();
        given(contextResolver.fromChapterId(CHAPTER_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByChapterId(CHAPTER_ID, USER_ID, StudyAction.WRITE_STUDY_CONTENT);

        verify(policy).authorize(context, StudyAction.WRITE_STUDY_CONTENT);
    }

    @Test
    void lectureId로_context를_만들고_권한을_검사한다() {
        StudyAccessContext context = context();
        given(contextResolver.fromLectureId(LECTURE_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByLectureId(LECTURE_ID, USER_ID, StudyAction.WRITE_STUDY_CONTENT);

        verify(policy).authorize(context, StudyAction.WRITE_STUDY_CONTENT);
    }

    private StudyAccessContext context() {
        return new StudyAccessContext(
                STUDY_ID,
                COURSE_ID,
                USER_ID,
                StudyStatus.ACTIVE,
                true,
                true,
                StudyMemberRole.MEMBER
        );
    }
}
