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

    //최근 시청내역 갱신
    @Transactional
    public void record(Long userId, Long courseId, Long lectureId) {
        lastWatchedLectureRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresentOrElse(
                        entry -> entry.changeLecture(lectureId),    //이전내역있으면 갱신
                        () -> lastWatchedLectureRepository.save(
                                LastWatchedLecture.of(userId, courseId, lectureId))     //없으면 생성
                );
    }

    public Optional<Long> findLectureId(Long userId, Long courseId) {
        return lastWatchedLectureRepository.findByUserIdAndCourseId(userId, courseId)
                .map(LastWatchedLecture::getLectureId);
    }
}
