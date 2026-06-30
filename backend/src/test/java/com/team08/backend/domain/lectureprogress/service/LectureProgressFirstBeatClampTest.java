package com.team08.backend.domain.lectureprogress.service;

import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 동시 "첫 비트" 과다 계상 방어 단위 테스트.
 * <p>
 * 첫 비트는 previousBeatAt=null 이라 벽시계 클램프가 면제된다. 같은 강의를 탭 두 개로 동시에
 * 처음 열면 둘 다 "행 없음"을 보고 각자 첫 비트로 면제받아, 실제 경과 시간 없이도 watchedSeconds 가
 * 비트 수만큼 부풀 수 있다(유실이 아니라 과다). 방어책은 "이번 호출이 실제로 행을 만들었는지"로
 * 클램프 기준을 정하는 것 — 내가 만든 진짜 첫 비트만 면제하고, 남이 먼저 만든 경우엔 그 행의
 * 시각으로 클램프한다. 이 분기는 동시성에서만 도달하므로 협력자를 모킹해 결정적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class LectureProgressFirstBeatClampTest {

    @Mock private LectureProgressRepository lectureProgressRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private EnrollmentQueryService enrollmentQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Lecture lecture;

    @InjectMocks private LectureProgressService service;

    private static final long USER_ID = 1L;
    private static final long LECTURE_ID = 10L;
    private static final int DURATION = 600;

    @Test
    @DisplayName("남이 먼저 만든 행이면(insertIfAbsent=0) 첫 비트라도 그 행 시각으로 클램프해 과다 계상을 막는다")
    void firstBeat_rowCreatedByPeer_clampsDelta() {
        LocalDateTime eventTime = LocalDateTime.of(2026, 6, 13, 10, 0, 0);
        // 동시 첫 비트의 상대가 이미 만들어 둔 행: 같은 eventTime, 이미 30초 적재.
        LectureProgress peerRow = new LectureProgress(
                1L, LECTURE_ID, USER_ID, 100, 30, 5, false, null, eventTime, eventTime);

        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(true);
        given(lecture.getDurationSeconds()).willReturn(DURATION);
        // 1st find: 없음 → 생성 시도. insertIfAbsent=0 (상대가 먼저 만듦). 2nd find: 상대 행.
        given(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, LECTURE_ID))
                .willReturn(Optional.empty(), Optional.of(peerRow));
        given(lectureProgressRepository.insertIfAbsent(anyLong(), anyLong(), anyInt(), any())).willReturn(0);
        given(lectureProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // 같은 eventTime 에 600초를 주장하지만, 상대 행의 시각으로 클램프 → 경과 0 → delta 0.
        LectureProgress result = service.applyProgress(USER_ID, LECTURE_ID, 120, 600, eventTime);

        // 과다 계상(30+600=630) 없이 그대로 30 유지.
        assertThat(result.getWatchedSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("내가 만든 진짜 첫 비트면(insertIfAbsent=1) 면제 유지 — delta 가 클램프 없이 그대로 반영된다")
    void firstBeat_rowCreatedByMe_keepsUnclamped() {
        LocalDateTime eventTime = LocalDateTime.of(2026, 6, 13, 10, 0, 0);
        LectureProgress freshRow = new LectureProgress(
                1L, LECTURE_ID, USER_ID, 0, 0, 0, false, null, eventTime, eventTime);

        given(lectureRepository.findByIdWithChapterAndCourse(LECTURE_ID)).willReturn(Optional.of(lecture));
        given(lecture.isFreePreview()).willReturn(true);
        given(lecture.getDurationSeconds()).willReturn(DURATION);
        given(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, LECTURE_ID))
                .willReturn(Optional.empty(), Optional.of(freshRow));
        given(lectureProgressRepository.insertIfAbsent(anyLong(), anyLong(), anyInt(), any())).willReturn(1);
        given(lectureProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        LectureProgress result = service.applyProgress(USER_ID, LECTURE_ID, 45, 45, eventTime);

        // 진짜 첫 비트라 클램프 면제 → 45 그대로.
        assertThat(result.getWatchedSeconds()).isEqualTo(45);
    }
}
