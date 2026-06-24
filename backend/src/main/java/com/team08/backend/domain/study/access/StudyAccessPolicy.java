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
            case VIEW_STUDY_CONTENT ->
                    context.isActiveMember()
                            && context.isReadableStudy();
            case WRITE_STUDY_CONTENT ->
                    context.isActiveMember()
                            && context.isWritableStudy();
        };
    }
}
