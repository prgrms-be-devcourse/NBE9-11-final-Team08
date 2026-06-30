package com.team08.backend.domain.lectureprogress.integration;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.lectureprogress.service.LectureProgressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 (user, lecture) 진행 행에 동시 하트비트가 겹쳐도 낙관적 락 + 재시도가
 * read-modify-write 충돌을 흡수하는지 실제 DB(Testcontainers MySQL)로 검증한다.
 * <p>
 * 진행 행을 미리 심어 두고(=순수 UPDATE 경로만 경합) 모든 비트에 같은 eventTime 을 준다.
 * 그러면 결과가 결정적이다: 첫 커밋만 delta 를 반영(이후 비트는 벽시계 클램프로 0)하므로
 * 최종 watchedSeconds 는 정확히 1회분(DELTA)이고, 낙관적 락이 없으면 두 트랜잭션이 같은
 * 버전을 읽고 각자 더해 한쪽이 사라지는 lost update 가 발생한다. 동시 first-beat(INSERT 경합)는
 * 별개의 데드락 이슈가 있어 이 테스트 범위에서 제외한다(행을 미리 심어 UPDATE 만 경합시킨다).
 */
@SpringBootTest
class LectureProgressConcurrencyTest {

    @Autowired private LectureProgressService lectureProgressService;
    @Autowired private LectureProgressRepository lectureProgressRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private LectureRepository lectureRepository;

    private static final long USER_ID = 9_900_001L;
    private static final int DURATION = 600;
    private static final int DELTA = 30;

    @Test
    @DisplayName("동시 하트비트가 같은 행을 갱신해도 낙관적 락 재시도로 유실 없이 정확히 1회분만 반영된다")
    void concurrentHeartbeats_noLostUpdate() throws InterruptedException {
        Long lectureId = persistFreePreviewLecture();
        LocalDateTime eventTime = LocalDateTime.now();
        // 진행 행을 미리 심는다(updatedAt 을 충분히 과거로 둬 첫 비트의 클램프가 delta 를 자르지 않게).
        lectureProgressRepository.saveAndFlush(
                LectureProgress.start(USER_ID, lectureId, 0, eventTime.minusSeconds(120)));

        int threadCount = 3; // 최악 재시도 깊이(threadCount-1=2)가 MAX_WRITE_ATTEMPTS(3) 안에 든다.
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 모든 스레드를 동시에 출발시켜 같은 행 경합을 최대화
                    lectureProgressService.applyHeartbeat(USER_ID, lectureId, 120, DELTA, eventTime);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 모든 비트가 재시도로 충돌을 흡수하고 성공 — 예외로 샌 비트가 없다.
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();

        LectureProgress progress =
                lectureProgressRepository.findByUserIdAndLectureId(USER_ID, lectureId).orElseThrow();
        // 같은 eventTime → 첫 커밋만 delta 반영, 나머지는 클램프로 0. 정확히 1회분만 누적(유실/중복 없음).
        assertThat(progress.getWatchedSeconds()).isEqualTo(DELTA);
        assertThat(progress.getCompleted()).isFalse();
        // 여러 트랜잭션이 같은 행을 실제로 직렬 갱신했음(낙관적 락 동작).
        assertThat(progress.getVersion()).isNotNull();
        assertThat(progress.getVersion()).isPositive();
    }

    private Long persistFreePreviewLecture() {
        Course course = courseRepository.save(
                Course.createDraft(99L, 1L, "동시성 코스", "설명", "thumb", 10000));
        Chapter chapter = chapterRepository.save(Chapter.create("챕터", 1, course));
        Lecture lecture = lectureRepository.save(
                Lecture.createDraft("강의", "요약", DURATION, 1, true, chapter));
        return lecture.getId();
    }
}
