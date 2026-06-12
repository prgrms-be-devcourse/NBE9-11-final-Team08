package com.team08.backend.domain.course.service;

import com.team08.backend.domain.course.event.CourseClosedEvent;
import com.team08.backend.domain.course.event.AdminCourseRejectedEvent;
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
        // given
        Long courseId = 100L;
        CourseClosedEvent event = new CourseClosedEvent(courseId);

        // when
        courseClosedEventListener.handleCourseClosedEvent(event);

        // then
        verify(courseStudyManager).closeForCourse(courseId);
    }

    @Test
    void 강좌_심사_반려_이벤트를_수신하면_연관된_스터디를_비활성화하도록_매니저에게_위임한다() {
        // given
        Long courseId = 100L;
        AdminCourseRejectedEvent event = new AdminCourseRejectedEvent(courseId);

        // when
        courseClosedEventListener.handleAdminCourseRejectedEvent(event);

        // then
        verify(courseStudyManager).rejectForCourse(courseId);
    }
}