package com.team08.backend.domain.lectureprogress.repository;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {

    Optional<LectureProgress> findByUserIdAndLectureId(Long userId, Long lectureId);

    /**
     * 하트비트 누적(watchedSeconds += delta)용 쓰기 락 조회.
     * 같은 (user, lecture) 행에 대한 동시 하트비트가 각자 읽고 더한 뒤 저장하면 한쪽 delta 가
     * 사라지는 lost update 가 발생한다. SELECT ... FOR UPDATE 로 행을 잠가
     * read-modify-write 를 행 단위로 직렬화한다(서로 다른 행은 경합하지 않음).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM LectureProgress p WHERE p.userId = :userId AND p.lectureId = :lectureId")
    Optional<LectureProgress> findByUserIdAndLectureIdForUpdate(@Param("userId") Long userId,
                                                               @Param("lectureId") Long lectureId);

    /**
     * 진행 행을 race-safe 하게 생성한다. (user_id, lecture_id) 유니크 제약 위에서
     * ON DUPLICATE KEY UPDATE 를 no-op 으로 두어, 동시 입장으로 행이 이미 있어도
     * 예외 없이 무시한다(현재 트랜잭션을 rollback-only 로 만들지 않음).
     * 삽입 후에는 findByUserIdAndLectureId 로 재조회해 managed 엔티티를 얻는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO lecture_progresses
                (user_id, lecture_id, last_position_seconds, watched_seconds,
                 progress_rate, completed, created_at, updated_at)
            VALUES (:userId, :lectureId, :positionSeconds, 0, 0, false, :now, :now)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId,
                        @Param("lectureId") Long lectureId,
                        @Param("positionSeconds") int positionSeconds,
                        @Param("now") LocalDateTime now);

    // 강좌 커리큘럼 화면용 — 사용자의 강좌 내 모든 강의 진행 정보(있는 것만)
    List<LectureProgress> findByUserIdAndLectureIdIn(Long userId, List<Long> lectureIds);

    // 사용자의 강좌 내 완료한 강의 수
    long countByUserIdAndLectureIdInAndCompleted(Long userId, List<Long> lectureIds, Boolean completed);

    // 강좌 내 총 시청 시간(초) — 강의당 1행만 존재하므로 단순 SUM (이벤트 중복 합산 없음)
    @Query("SELECT COALESCE(SUM(p.watchedSeconds), 0) FROM LectureProgress p " +
           "WHERE p.userId = :userId AND p.lectureId IN :lectureIds")
    int sumWatchedSecondsByUserIdAndLectureIdIn(@Param("userId") Long userId,
                                                @Param("lectureIds") List<Long> lectureIds);

    // 시청 시간 기준 상위 강의 진행 정보 (TOP3용)
    List<LectureProgress> findTop3ByUserIdAndLectureIdInOrderByWatchedSecondsDesc(Long userId, List<Long> lectureIds);
}
