package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.course.event.AdminCourseRejectedEvent;
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
    void 강좌_폐쇄_이벤트를_수신하면_연관된_스터디를_종료하도록_매니저에게_위임한다() {
        Long courseId = 100L;
        CourseClosedEvent event = new CourseClosedEvent(courseId);

        courseClosedEventListener.handleCourseClosedEvent(event);

        verify(courseStudyManager).closeForCourse(courseId);
    }

    @Test
    void 강좌_심사_반려_이벤트를_수신하면_연관된_스터디를_비활성화하도록_매니저에게_위임한다() {
        Long courseId = 100L;
        AdminCourseRejectedEvent event = new AdminCourseRejectedEvent(courseId);

        courseClosedEventListener.handleAdminCourseRejectedEvent(event);

        verify(courseStudyManager).rejectForCourse(courseId);
    }

    @Test
    void 강좌_삭제_이벤트를_수신하면_연관된_스터디를_비활성화하도록_매니저에게_위임한다() {
        Long courseId = 100L;
        CourseDeletedEvent event = new CourseDeletedEvent(courseId);

        courseClosedEventListener.handleCourseDeletedEvent(event);

        verify(courseStudyManager).rejectForCourse(courseId);
    }
}