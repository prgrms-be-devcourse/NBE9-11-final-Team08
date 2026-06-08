package com.team08.backend.domain.lecture.entity;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LectureProgress 단위 테스트")
class LectureProgressTest {

    private Lecture lecture;
    private User user;

    @BeforeEach
    void setUp() {
        user = TestEntityFactory.user("learner@test.com", "학습자");
        Chapter chapter = TestEntityFactory.chapter(null, "1장", 1);
        lecture = TestEntityFactory.lecture(chapter, "강의1", "video-1", 1000, 1);
    }

    @Test
    @DisplayName("시청 완료되지 않은 강의는 마지막 시청 위치 기반 진행률을 제공한다")
    void getProgressPercent_incomplete() {
        LectureProgress progress = LectureProgress.start(lecture, user);
        progress.updatePosition(250);

        assertThat(progress.getCompleted()).isFalse();
        assertThat(progress.getProgressPercent()).isEqualTo(25);
    }

    @Test
    @DisplayName("영상 길이의 95% 이상 시청하면 수강 완료 처리된다")
    void updatePosition_autoCompleteAt95Percent() {
        LectureProgress progress = LectureProgress.start(lecture, user);

        boolean newlyCompleted = progress.updatePosition(950);

        assertThat(newlyCompleted).isTrue();
        assertThat(progress.getCompleted()).isTrue();
        assertThat(progress.getProgressPercent()).isEqualTo(100);
        assertThat(progress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("강제 완료 처리 시 수강 완료 상태가 된다")
    void complete_forceComplete() {
        LectureProgress progress = LectureProgress.start(lecture, user);

        boolean newlyCompleted = progress.complete();

        assertThat(newlyCompleted).isTrue();
        assertThat(progress.getCompleted()).isTrue();
        assertThat(progress.getLastPositionSeconds()).isEqualTo(1000);
        assertThat(progress.getWatchedSeconds()).isEqualTo(1000);
    }

    @Test
    @DisplayName("이미 완료된 강의는 재완료 처리되지 않는다")
    void complete_alreadyCompleted() {
        LectureProgress progress = LectureProgress.start(lecture, user);
        progress.complete();

        boolean newlyCompleted = progress.complete();

        assertThat(newlyCompleted).isFalse();
    }

    @Test
    @DisplayName("재생 위치는 영상 길이를 초과할 수 없다")
    void updatePosition_clampsToDuration() {
        LectureProgress progress = LectureProgress.start(lecture, user);

        progress.updatePosition(5000);

        assertThat(progress.getLastPositionSeconds()).isEqualTo(1000);
    }

    @Test
    @DisplayName("누적 시청 시간은 이전 최대값보다 작아지지 않는다")
    void updatePosition_watchedSecondsMonotonic() {
        LectureProgress progress = LectureProgress.start(lecture, user);
        progress.updatePosition(600);
        progress.updatePosition(300);

        assertThat(progress.getWatchedSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("영상 길이가 0이면 진행률은 0이다")
    void getProgressPercent_zeroDuration() {
        ReflectionTestUtils.setField(lecture, "durationSeconds", 0);
        LectureProgress progress = LectureProgress.start(lecture, user);

        assertThat(progress.getProgressPercent()).isEqualTo(0);
    }
}
