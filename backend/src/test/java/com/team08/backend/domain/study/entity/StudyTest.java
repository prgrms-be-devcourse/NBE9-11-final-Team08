package com.team08.backend.domain.study.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.study.exception.InvalidStudyStatusTransitionException;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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

    @Test
    void DRAFT_상태인_스터디는_ACTIVE_상태로_변경할_수_있다() {
        // given
        Study study = TestEntityFactory.draftStudy(1L);

        // when
        study.activate();

        // then
        assertThat(study.getStatus()).isEqualTo(StudyStatus.ACTIVE);
    }

    @Test
    void DRAFT_상태가_아닌_스터디는_ACTIVE_상태로_변경할_수_없다() {
        // given
        Study study = TestEntityFactory.readOnlyStudy(1L);

        // when
        Throwable thrown = catchThrowable(study::activate);

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidStudyStatusTransitionException.class);
    }
}
