package com.team08.backend.domain.studyactivity.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyActivityTest {

    @Test
    void 스터디_활동을_생성한다() {
        Long studyId = 1L;
        Long authorId = 2L;
        String content = "오늘 학습한 내용을 스터디원들과 공유합니다.";

        StudyActivity activity = StudyActivity.create(studyId, authorId, content);

        assertThat(activity.getStudyId()).isEqualTo(studyId);
        assertThat(activity.getAuthorId()).isEqualTo(authorId);
        assertThat(activity.getContent()).isEqualTo(content);
        assertThat(activity.getDeletedAt()).isNull();
    }

    @Test
    void 스터디_활동_내용을_수정한다() {
        StudyActivity activity = StudyActivity.create(
                1L,
                2L,
                "수정하기 전 스터디 활동 내용입니다."
        );

        activity.update("수정한 이후의 스터디 활동 내용입니다.");

        assertThat(activity.getContent())
                .isEqualTo("수정한 이후의 스터디 활동 내용입니다.");
    }
}
