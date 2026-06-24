package com.team08.backend.domain.study.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyAccessAuthorizer {

    private final StudyAccessContextResolver contextResolver;
    private final StudyAccessPolicy policy;

    public void authorizeByStudyId(Long studyId, Long userId, StudyAction action) {
        policy.authorize(contextResolver.fromStudyId(studyId, userId), action);
    }

    public void authorizeByCourseId(Long courseId, Long userId, StudyAction action) {
        policy.authorize(contextResolver.fromCourseId(courseId, userId), action);
    }
}
