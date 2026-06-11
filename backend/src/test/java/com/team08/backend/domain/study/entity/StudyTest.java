package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudyTest {

    @Test
    void 강좌용_스터디는_DRAFT_상태로_생성된다() {
        // given
        User owner = TestEntityFactory.user(1L);
        Course course = TestEntityFactory.course(10L);

        // when
        Study study = Study.createForCourse(
                owner,
                course,
                "스프링 강좌 스터디",
                "강좌 기반 스터디"
        );

        // then
        assertThat(study.getStatus()).isEqualTo(StudyStatus.DRAFT);
    }
}
