package com.team08.backend.domain.study.access;

import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyAccessPolicyTest {

    private final StudyAccessPolicy policy = new StudyAccessPolicy();

    @Test
    void ACTIVE_스터디의_활성_수강권과_활성_멤버는_조회와_쓰기가_가능하다() {
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
                true,
                true,
                StudyMemberRole.MEMBER
        );

        policy.authorize(context, StudyAction.VIEW_STUDY_CONTENT);
        policy.authorize(context, StudyAction.WRITE_STUDY_CONTENT);
    }

    @Test
    void READONLY_스터디는_조회만_가능하고_쓰기는_거부된다() {
        StudyAccessContext context = context(
                StudyStatus.READONLY,
                true,
                true,
                StudyMemberRole.MEMBER
        );

        policy.authorize(context, StudyAction.VIEW_STUDY_CONTENT);

        assertDenied(() -> policy.authorize(context, StudyAction.WRITE_STUDY_CONTENT));
    }

    @Test
    void 스터디_OWNER는_멤버_리포트를_조회할_수_있다() {
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
                false,
                true,
                StudyMemberRole.OWNER
        );

        policy.authorize(context, StudyAction.VIEW_MEMBER_REPORT);
    }

    @Test
    void 일반_멤버는_멤버_리포트를_조회할_수_없다() {
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
                true,
                true,
                StudyMemberRole.MEMBER
        );

        assertDenied(() -> policy.authorize(context, StudyAction.VIEW_MEMBER_REPORT));
    }

    @Test
    void 스터디_OWNER는_수강권_없이도_QnA_답변이_가능하다() {
        // 강사는 자기 강의를 결제하지 않으므로 enrollment 가 없어도 통과해야 한다.
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
                false,
                true,
                StudyMemberRole.OWNER
        );

        policy.authorize(context, StudyAction.MANAGE_ANSWER);
    }

    @Test
    void OWNER가_아니면_QnA_답변이_거부된다() {
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
                true,
                true,
                StudyMemberRole.MEMBER
        );

        assertDenied(() -> policy.authorize(context, StudyAction.MANAGE_ANSWER));
    }

    @Test
    void DRAFT_스터디_OWNER는_커리큘럼을_관리할_수_있다() {
        StudyAccessContext context = context(
                StudyStatus.DRAFT,
                false,
                true,
                StudyMemberRole.OWNER
        );

        policy.authorize(context, StudyAction.MANAGE_CURRICULUM);
    }

    @Test
    void DRAFT_스터디의_OWNER가_아니면_커리큘럼을_관리할_수_없다() {
        StudyAccessContext context = context(
                StudyStatus.DRAFT,
                true,
                true,
                StudyMemberRole.MEMBER
        );

        assertDenied(() -> policy.authorize(context, StudyAction.MANAGE_CURRICULUM));
    }

    private StudyAccessContext context(
            StudyStatus studyStatus,
            boolean hasActiveEnrollment,
            boolean activeMember,
            StudyMemberRole memberRole
    ) {
        return new StudyAccessContext(
                1L,
                10L,
                100L,
                studyStatus,
                hasActiveEnrollment,
                activeMember,
                memberRole
        );
    }

    private void assertDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_ACCESS_DENIED);
    }
}
