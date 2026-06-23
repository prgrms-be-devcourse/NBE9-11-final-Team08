package com.team08.backend.domain.lastwatchedlecture.service;

import com.team08.backend.domain.lastwatchedlecture.entity.LastWatchedLecture;
import com.team08.backend.domain.lastwatchedlecture.repository.LastWatchedLectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LastWatchedLectureService {

    private final LastWatchedLectureRepository lastWatchedLectureRepository;

    //최근 시청내역 갱신 — 동시 입장 레이스에도 안전하도록 원자적 UPSERT 로 처리
    @Transactional
    public void record(Long userId, Long courseId, Long lectureId) {
        lastWatchedLectureRepository.upsert(userId, courseId, lectureId);
    }

    public Optional<Long> findLectureId(Long userId, Long courseId) {
        return lastWatchedLectureRepository.findByUserIdAndCourseId(userId, courseId)
                .map(LastWatchedLecture::getLectureId);
    }
}
