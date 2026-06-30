package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.course.event.CourseDeletedEvent;
import com.team08.backend.domain.study.service.CourseStudyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseClosedEventListenerTest {

    @Mock
    private CourseStudyManager courseStudyManager;

    @InjectMocks
    private CourseClosedEventListener courseClosedEventListener;

    @Test
    void courseClosedEventDelegatesToCloseStudyForCourse() {
        Long courseId = 100L;
        CourseClosedEvent event = new CourseClosedEvent(courseId);

        courseClosedEventListener.handleCourseClosedEvent(event);

        verify(courseStudyManager).closeForCourse(courseId);
    }

    @Test
    void courseDeletedEventDelegatesToRejectStudyForCourse() {
        Long courseId = 100L;
        CourseDeletedEvent event = new CourseDeletedEvent(courseId);

        courseClosedEventListener.handleCourseDeletedEvent(event);

        verify(courseStudyManager).rejectForCourse(courseId);
    }
}
