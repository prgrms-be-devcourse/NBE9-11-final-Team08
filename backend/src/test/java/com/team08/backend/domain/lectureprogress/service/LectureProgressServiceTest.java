package com.team08.backend.domain.lectureprogress.service;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.service.EnrollmentQueryService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.global.config.JpaConfig;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 진행(LectureProgress) 갱신 동작을 실제 DB(Testcontainers MySQL)로 검증한다.
 * 수강권 검증은 진행 행이 처음 생성될 때만 수행한다(무료 맛보기는 예외).
 */
@DataJpaTest
@Import({JpaConfig.class, LectureProgressService.class, EnrollmentQueryService.class})
class LectureProgressServiceTest {

    @Autowired private LectureProgressService lectureProgressService;
    @Autowired private LectureProgressRepository lectureProgressRepository;
    @Autowired private TestEntityManager em;

    private static final Long USER_ID = 1L;
    private static final int DURATION = 600;

    // ── 픽스처 ────────────────────────────────────────────────────────────
    private Lecture persistLecture(boolean freePreview) {
        Course course = Course.createDraft(99L, 1L, "코스", "설명", "thumb", 10000);
        em.persist(course);
        Chapter chapter = Chapter.create("챕터", 1, course);
        em.persist(chapter);
        Lecture lecture = Lecture.createDraft("강의", "요약", DURATION, 1, freePreview, chapter);
        em.persist(lecture);
        em.flush();
        return lecture;
    }

    private void enroll(Long userId, Long courseId) {
        Order order = Order.createPendingPayment(userId, "ORD-" + userId + "-" + courseId, LocalDateTime.now());
        em.persist(order);
        em.persist(Enrollment.createActive(userId, courseId, order, LocalDateTime.now()));
        em.flush();
    }

    private Long courseIdOf(Lecture lecture) {
        return lecture.getChapter().getCourse().getId();
    }

    // ── 퇴장 upsert (무료 맛보기로 수강권 검증 없이 검증) ──────────────────
    @Test
    @DisplayName("진행 정보가 없으면 새로 생성하며 마지막 위치를 저장한다")
    void upsert_create_whenNoProgress() {
        Long lectureId = persistLecture(true).getId();
        LocalDateTime exitTime = LocalDateTime.of(2026, 6, 13, 10, 0);

        lectureProgressService.upsertLastPosition(USER_ID, lectureId, 150, exitTime);

        Optional<LectureProgress> saved =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getLastPositionSeconds()).isEqualTo(150);
        assertThat(saved.get().getUpdatedAt()).isEqualTo(exitTime);
    }

    @Test
    @DisplayName("진행 정보가 있으면 행을 추가하지 않고 위치/갱신시각만 업데이트한다")
    void upsert_update_whenProgressExists() {
        Long lectureId = persistLecture(true).getId();

        lectureProgressService.upsertLastPosition(
                USER_ID, lectureId, 100, LocalDateTime.of(2026, 6, 13, 10, 0));

        LocalDateTime laterExit = LocalDateTime.of(2026, 6, 13, 11, 30);
        lectureProgressService.upsertLastPosition(USER_ID, lectureId, 420, laterExit);

        // unique(user_id, lecture_id) — 행은 1개만 유지
        assertThat(lectureProgressRepository.count()).isEqualTo(1);

        LectureProgress updated =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        assertThat(updated.getLastPositionSeconds()).isEqualTo(420);
        assertThat(updated.getUpdatedAt()).isEqualTo(laterExit);
    }

    // ── 하트비트 + 수강권 검증 ────────────────────────────────────────────
    @Test
    @DisplayName("수강권이 있으면 하트비트로 진행 행을 생성하고 시청시간을 누적한다")
    void heartbeat_enrolled_createsAndAccumulates() {
        Lecture lecture = persistLecture(false);
        Long lectureId = lecture.getId();
        enroll(USER_ID, courseIdOf(lecture));

        lectureProgressService.applyHeartbeat(USER_ID, lectureId, 120, 30, LocalDateTime.now());

        LectureProgress progress =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        assertThat(progress.getWatchedSeconds()).isEqualTo(30);
        assertThat(progress.getLastPositionSeconds()).isEqualTo(120);
        assertThat(progress.getProgressRate()).isEqualTo(5); // 시청 30초 / 600초 = 5%
        assertThat(progress.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("수강권 없는 비(非)무료 강의 하트비트는 차단되고 진행 행이 생성되지 않는다")
    void heartbeat_notEnrolledNonPreview_throwsAndCreatesNothing() {
        Long lectureId = persistLecture(false).getId();

        assertThatThrownBy(() ->
                lectureProgressService.applyHeartbeat(USER_ID, lectureId, 120, 30, LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.VIDEO_ACCESS_DENIED.getMessage());

        assertThat(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId)).isEmpty();
    }

    @Test
    @DisplayName("무료 맛보기 강의는 수강권 없이도 하트비트로 진행을 누적한다")
    void heartbeat_freePreview_allowsWithoutEnrollment() {
        Long lectureId = persistLecture(true).getId();

        lectureProgressService.applyHeartbeat(USER_ID, lectureId, 60, 60, LocalDateTime.now());

        LectureProgress progress =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        assertThat(progress.getWatchedSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("이미 진행 행이 있으면 수강권이 없어도 검증을 건너뛰고 갱신한다 (back 배치 검증)")
    void heartbeat_existingRow_skipsEnrollmentCheck() {
        Long lectureId = persistLecture(false).getId();
        LocalDateTime now = LocalDateTime.now();
        // 수강권 없이, 사전에 진행 행을 직접 심어 둔다(과거에 접근 권한이 있었다고 가정).
        // 직전 비트를 충분히 과거(120초 전)로 둬 벽시계 경계가 delta 40 을 자르지 않도록 한다.
        lectureProgressRepository.save(LectureProgress.start(USER_ID, lectureId, 0, now.minusSeconds(120)));

        lectureProgressService.applyHeartbeat(USER_ID, lectureId, 90, 40, now);

        LectureProgress progress =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        assertThat(progress.getWatchedSeconds()).isEqualTo(40);
        assertThat(progress.getLastPositionSeconds()).isEqualTo(90);
    }

    @Test
    @DisplayName("벽시계 경과를 초과하는 delta 는 (경과×배속)으로 잘려 누적된다 (연타·재전송 방어)")
    void heartbeat_clampsDeltaToWallClockElapsed() {
        Long lectureId = persistLecture(false).getId();
        LocalDateTime now = LocalDateTime.now();
        // 직전 비트가 10초 전인데 delta 600 을 주장 → 허용치는 10초 × 2배 = 20초
        lectureProgressRepository.save(LectureProgress.start(USER_ID, lectureId, 0, now.minusSeconds(10)));

        lectureProgressService.applyHeartbeat(USER_ID, lectureId, 300, 600, now);

        LectureProgress progress =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        assertThat(progress.getWatchedSeconds()).isEqualTo(20); // min(600, 10초×2)
    }

    // ── 입장 시 진행 행 보장 (ensureStarted) ──────────────────────────────
    @Test
    @DisplayName("입장 시 수강권이 있으면 진행 행을 서버 시각으로 생성한다")
    void ensureStarted_enrolled_createsRow() {
        Lecture lecture = persistLecture(false);
        enroll(USER_ID, courseIdOf(lecture));
        LocalDateTime now = LocalDateTime.of(2026, 6, 13, 10, 0);

        LectureProgress created = lectureProgressService.ensureStarted(USER_ID, lecture, now);

        assertThat(created).isNotNull();
        assertThat(created.getWatchedSeconds()).isZero();
        assertThat(created.getUpdatedAt()).isEqualTo(now);   // 첫 하트비트의 벽시계 기준점
        assertThat(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lecture.getId())).isPresent();
    }

    @Test
    @DisplayName("입장 시 미등록·비무료면 행을 만들지 않고 null 을 반환한다(입장 자체는 허용)")
    void ensureStarted_notEnrolled_returnsNullNoRow() {
        Lecture lecture = persistLecture(false);

        LectureProgress result = lectureProgressService.ensureStarted(USER_ID, lecture, LocalDateTime.now());

        assertThat(result).isNull();
        assertThat(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lecture.getId())).isEmpty();
    }

    @Test
    @DisplayName("입장 시 무료 맛보기는 미등록이어도 진행 행을 생성한다")
    void ensureStarted_freePreview_createsRowWithoutEnrollment() {
        Lecture lecture = persistLecture(true);

        LectureProgress created = lectureProgressService.ensureStarted(USER_ID, lecture, LocalDateTime.now());

        assertThat(created).isNotNull();
        assertThat(lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lecture.getId())).isPresent();
    }

    @Test
    @DisplayName("입장 시 진행 행이 이미 있으면 그대로 반환한다(재생성 안 함)")
    void ensureStarted_existing_returnsSame() {
        Lecture lecture = persistLecture(false);
        LectureProgress seeded = lectureProgressRepository.save(
                LectureProgress.start(USER_ID, lecture.getId(), 30, LocalDateTime.now().minusMinutes(1)));

        LectureProgress result = lectureProgressService.ensureStarted(USER_ID, lecture, LocalDateTime.now());

        assertThat(result.getId()).isEqualTo(seeded.getId());
        assertThat(lectureProgressRepository.count()).isEqualTo(1);
    }
}
