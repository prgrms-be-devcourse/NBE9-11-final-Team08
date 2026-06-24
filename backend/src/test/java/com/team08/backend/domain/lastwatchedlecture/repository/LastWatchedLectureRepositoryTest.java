package com.team08.backend.domain.lastwatchedlecture.repository;

import com.team08.backend.domain.lastwatchedlecture.entity.LastWatchedLecture;
import com.team08.backend.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 강좌별 마지막 시청 강의 UPSERT 회귀 테스트.
 * <p>
 * 과거 record() 는 "조회 후 없으면 save" 방식이라 강의 입장이 동시에 들어오면
 * (user_id, course_id) 유니크 제약에 Duplicate entry 가 터졌다. 이는
 * (1) 실제 DB 유니크 제약과 (2) 동시성이 함께 있어야 재현되는데,
 * 기존 테스트는 서비스/리포지토리를 목으로 막아 단일 스레드라 잡지 못했다.
 * 여기서는 실제 MySQL(Testcontainers)에서 동시 호출로 검증한다.
 */
@DataJpaTest
@Import(JpaConfig.class)
class LastWatchedLectureRepositoryTest {

    @Autowired
    private LastWatchedLectureRepository repository;

    @Test
    @DisplayName("같은 (user, course) 로 upsert 를 반복하면 1행만 유지되고 lecture_id 가 갱신된다")
    void upsert_sameKey_keepsSingleRowAndUpdatesLecture() {
        repository.upsert(7L, 3L, 100L);
        repository.upsert(7L, 3L, 200L);

        Optional<LastWatchedLecture> found = repository.findByUserIdAndCourseId(7L, 3L);
        assertThat(found).isPresent();
        assertThat(found.get().getLectureId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("동시에 같은 (user, course) upsert 가 들어와도 Duplicate 없이 1행만 유지된다")
    void upsert_concurrentSameKey_noDuplicate() throws InterruptedException {
        long userId = 77L;
        long courseId = 33L;
        int threadCount = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final long lectureId = i + 1;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();              // 모든 스레드가 동시에 출발
                    repository.upsert(userId, courseId, lectureId);
                } catch (Exception e) {
                    failures.incrementAndGet(); // Duplicate entry 등 발생 시 실패로 집계
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        boolean finished = done.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(finished).isTrue();
        assertThat(failures.get()).isZero();                         // 충돌 예외 없음
        assertThat(repository.findByUserIdAndCourseId(userId, courseId)).isPresent(); // 정확히 1행
    }
}
