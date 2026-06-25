package com.team08.backend.domain.course.access;

import com.team08.backend.domain.course.entity.CourseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseAccessAuthorizerTest {

    private static final Long COURSE_ID = 10L;
    private static final Long CHAPTER_ID = 20L;
    private static final Long LECTURE_ID = 30L;
    private static final Long USER_ID = 100L;

    @Mock
    private CourseAccessContextResolver contextResolver;

    @Mock
    private CourseAccessPolicy policy;

    @InjectMocks
    private CourseAccessAuthorizer authorizer;

    @Test
    void chapterId로_context를_만들고_권한을_검사한다() {
        CourseAccessContext context = context();
        given(contextResolver.fromChapterId(CHAPTER_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByChapterId(CHAPTER_ID, USER_ID, CourseAction.WRITE_CONTENT);

        verify(policy).authorize(context, CourseAction.WRITE_CONTENT);
    }

    @Test
    void lectureId로_context를_만들고_권한을_검사한다() {
        CourseAccessContext context = context();
        given(contextResolver.fromLectureId(LECTURE_ID, USER_ID)).willReturn(context);

        authorizer.authorizeByLectureId(LECTURE_ID, USER_ID, CourseAction.WRITE_CONTENT);

        verify(policy).authorize(context, CourseAction.WRITE_CONTENT);
    }

    private CourseAccessContext context() {
        return new CourseAccessContext(
                USER_ID,
                CourseStatus.ON_SALE,
                true,
                false,
                false,
                false
        );
    }
}
