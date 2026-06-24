package com.team08.backend.domain.lecture.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LectureDbService {

    private final LectureRepository lectureRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateLectureM3u8(Long lectureId, String dbSavePath, String targetDirName) {
        validateTargetDirName(targetDirName);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        lecture.updateM3u8Path(dbSavePath, targetDirName);

        eventPublisher.publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
    }

    private void validateTargetDirName(String targetDirName) {
        if (targetDirName == null || targetDirName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try {
            UUID.fromString(targetDirName);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}