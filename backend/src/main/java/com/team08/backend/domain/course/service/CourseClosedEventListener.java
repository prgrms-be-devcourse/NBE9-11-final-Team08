package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.study.service.CourseStudyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseClosedEventListener {

    private final CourseStudyManager courseStudyManager;

    @EventListener
    public void handleCourseClosedEvent(CourseClosedEvent event) {
        courseStudyManager.closeForCourse(event.courseId());
    }
}