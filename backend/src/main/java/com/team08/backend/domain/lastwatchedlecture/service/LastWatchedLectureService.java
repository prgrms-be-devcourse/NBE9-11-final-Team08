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

    @Transactional
    public void record(Long userId, Long courseId, Long lectureId) {
        lastWatchedLectureRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresentOrElse(
                        entry -> entry.changeLecture(lectureId),
                        () -> lastWatchedLectureRepository.save(
                                LastWatchedLecture.of(userId, courseId, lectureId))
                );
    }

    public Optional<Long> findLectureId(Long userId, Long courseId) {
        return lastWatchedLectureRepository.findByUserIdAndCourseId(userId, courseId)
                .map(LastWatchedLecture::getLectureId);
    }
}
