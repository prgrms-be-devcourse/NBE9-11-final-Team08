package com.team08.backend.domain.course.access;

import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseAccessPolicyTest {

    private final CourseAccessPolicy policy = new CourseAccessPolicy();

    @Test
    void Course_OWNER는_멤버_리포트를_조회할_수_있다() {
        CourseAccessContext context = context(
                CourseStatus.ON_SALE,
                false,
                true,
                false
        );

        policy.authorize(context, CourseAction.VIEW_MEMBER_REPORT);
    }

    @Test
    void 일반_멤버는_멤버_리포트를_조회할_수_없다() {
        CourseAccessContext context = context(
                CourseStatus.ON_SALE,
                true,
                false,
                false
        );

        assertDenied(() -> policy.authorize(context, CourseAction.VIEW_MEMBER_REPORT));
    }

    @Test
    void Course_OWNER는_수강권_없이도_QnA_답변이_가능하다() {
        // 강사는 자기 강의를 결제하지 않으므로 enrollment 가 없어도 통과해야 한다.
        CourseAccessContext context = context(
                CourseStatus.ON_SALE,
                false,
                true,
                false
        );

        policy.authorize(context, CourseAction.MANAGE_ANSWER);
    }

    @Test
    void OWNER가_아니면_QnA_답변이_거부된다() {
        CourseAccessContext context = context(
                CourseStatus.ON_SALE,
                false,
                false,
                false
        );

        assertDenied(() -> policy.authorize(context, CourseAction.MANAGE_ANSWER));
    }

    @Test
    void Course_OWNER는_커리큘럼을_관리할_수_있다() {
        CourseAccessContext context = context(
                CourseStatus.ON_SALE,
                false,
                true,
                false
        );

        policy.authorize(context, CourseAction.MANAGE_COURSE);
    }

    @Test
    void Course의_OWNER가_아니면_커리큘럼을_관리할_수_없다() {
        CourseAccessContext context = context(
                CourseStatus.DRAFT,
                false,
                false,
                false
        );

        assertDenied(() -> policy.authorize(context, CourseAction.MANAGE_COURSE));
    }

    private CourseAccessContext context(
            CourseStatus courseStatus,
            boolean hasActiveEnrollment,
            boolean isOwner,
            boolean hasFreePreview
    ) {
        return new CourseAccessContext(
                1L,
                courseStatus,
                hasActiveEnrollment,
                isOwner,
                hasFreePreview
        );
    }

    private void assertDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_ACCESS_DENIED);
    }
}
