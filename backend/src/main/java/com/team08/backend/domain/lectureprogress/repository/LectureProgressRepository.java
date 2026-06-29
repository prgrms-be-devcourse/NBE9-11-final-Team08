package com.team08.backend.domain.lectureprogress.repository;

import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {

    Optional<LectureProgress> findByUserIdAndLectureId(Long userId, Long lectureId);

    // 강좌 커리큘럼 화면용 — 사용자의 강좌 내 모든 강의 진행 정보(있는 것만)
    List<LectureProgress> findByUserIdAndLectureIdIn(Long userId, List<Long> lectureIds);

    // 강좌 내 강의들 중 사용자가 가장 최근 학습한 진행 정보
    Optional<LectureProgress> findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(Long userId, List<Long> lectureIds);

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
