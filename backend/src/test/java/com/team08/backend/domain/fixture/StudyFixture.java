package com.team08.backend.domain.fixture;

import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.entity.StudyVisibility;
import com.team08.backend.domain.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public final class StudyFixture {
    private StudyFixture() {}

    public static Study study(User owner) {
        return Study.create(
                owner,
                "스프링 스터디",
                "스프링 공부",
                StudyVisibility.PUBLIC,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
    }

    public static Study study(Long id, User owner) {
        Study study = study(owner);

        ReflectionTestUtils.setField(study, "id", id);

        return study;
    }

    public static Study inprogressStudy(Long id, User owner) {
        Study study = study(id, owner);

        ReflectionTestUtils.setField(study, "status", StudyStatus.IN_PROGRESS);

        return study;
    }
}
