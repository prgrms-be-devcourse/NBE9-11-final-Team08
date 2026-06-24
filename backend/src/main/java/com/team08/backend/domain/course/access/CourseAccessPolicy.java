package com.team08.backend.domain.course.access;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CourseAccessPolicy {

    public void authorize(CourseAccessContext context, CourseAction action) {
        if (!isAllowed(context, action)) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private boolean isAllowed(CourseAccessContext context, CourseAction action) {
        return switch (action) {
            case VIEW_CONTENT ->
                    (context.isReadableCourse() && context.hasActiveEnrollment()) || context.isOwner();
            case WRITE_CONTENT ->
                    (context.isWritableCourse() && context.hasActiveEnrollment()) || context.isOwner();
            case ENTER_COURSE ->
                    ((context.hasFreePreview() || context.hasActiveEnrollment()) && context.isReadableCourse())
                            || context.isOwner();
            case VIEW_MEMBER_REPORT, MANAGE_ANSWER ->
                    context.isReadableCourse() && context.isOwner();
            case MANAGE_COURSE ->
                    context.isOwner();
        };
    }
}
