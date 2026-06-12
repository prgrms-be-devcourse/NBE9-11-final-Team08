package com.team08.backend.domain.study.fixture;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.support.TestEntityFactory;
import org.springframework.test.util.ReflectionTestUtils;

public final class StudyFixture {
    private Long id = 1L;
    private User owner = TestEntityFactory.user(1L);
    private Course course = TestEntityFactory.course(1L);
    private String title = "스터디 제목";
    private String description = "스터디 설명";
    private StudyStatus status = StudyStatus.DRAFT;

    public static StudyFixture builder() {
        return new StudyFixture();
    }

    public Study build() {
        Study study = Study.createForCourse(owner, course, title, description);

        ReflectionTestUtils.setField(study, "id", id);

        return study;
    }

    public static Study draftStudy() {
        return StudyFixture.builder().build();
    }

    public static Study activeStudy() {
        Study study = draftStudy();
        study.activate();
        return study;
    }
}
