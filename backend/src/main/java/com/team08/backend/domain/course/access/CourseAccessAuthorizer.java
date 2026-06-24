package com.team08.backend.domain.course.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAccessAuthorizer {

    private final CourseAccessContextResolver contextResolver;
    private final CourseAccessPolicy policy;

    public void authorizeByCourseId(Long courseId, Long userId, CourseAction action) {
        policy.authorize(contextResolver.fromCourseId(courseId, userId), action);
    }

    public void authorizeByChapterId(Long chapterId, Long userId, CourseAction action) {
        policy.authorize(contextResolver.fromChapterId(chapterId, userId), action);
    }

    public void authorizeByLectureId(Long lectureId, Long userId, CourseAction action) {
        policy.authorize(contextResolver.fromLectureId(lectureId, userId), action);
    }
}
