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
    void ACTIVE_스터디의_활성_멤버는_조회와_쓰기가_가능하다() {
        StudyAccessContext context = context(
                StudyStatus.ACTIVE,
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
                StudyMemberRole.MEMBER
        );

        policy.authorize(context, StudyAction.VIEW_STUDY_CONTENT);

        assertDenied(() -> policy.authorize(context, StudyAction.WRITE_STUDY_CONTENT));
    }

    private StudyAccessContext context(
            StudyStatus studyStatus,
            boolean activeMember,
            StudyMemberRole memberRole
    ) {
        return new StudyAccessContext(
                1L,
                100L,
                studyStatus,
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
