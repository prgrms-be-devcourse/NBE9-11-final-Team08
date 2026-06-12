package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.command.CourseStudyCreateCommand;

public interface CourseStudyManager {
    Long createForCourse(CourseStudyCreateCommand command);
    void closeForCourse(Long courseId);
}