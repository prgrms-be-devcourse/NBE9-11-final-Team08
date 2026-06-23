package com.team08.backend.domain.study.access;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class StudyAccessPolicy {

    public void authorize(StudyAccessContext context, StudyAction action) {
        if (!isAllowed(context, action)) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private boolean isAllowed(StudyAccessContext context, StudyAction action) {
        return switch (action) {
            case VIEW_STUDY_CONTENT, VIEW_OWN_REPORT ->
                    hasReadableMemberAccess(context);
            case WRITE_STUDY_CONTENT, WRITE_OWN_STUDY_CONTENT, SAVE_REPORT_RECORD ->
                    hasWritableMemberAccess(context);
            case VIEW_MEMBER_REPORT ->
                    context.isReadableStudy()
                            && context.isActiveMember()
                            && context.isOwner();
            // QnA 답변: 강의 강사(= 스터디 OWNER)만. 강사는 수강권이 없으므로 enrollment 를 보지 않는다.
            case MANAGE_ANSWER ->
                    context.isReadableStudy()
                            && context.isActiveMember()
                            && context.isOwner();
            case MANAGE_CURRICULUM ->
                    context.isDraftStudy()
                            && context.isOwner();
        };
    }

    private boolean hasReadableMemberAccess(StudyAccessContext context) {
        return context.hasActiveEnrollment()
                && context.isActiveMember()
                && context.isReadableStudy();
    }

    private boolean hasWritableMemberAccess(StudyAccessContext context) {
        return context.hasActiveEnrollment()
                && context.isActiveMember()
                && context.isWritableStudy();
    }
}
