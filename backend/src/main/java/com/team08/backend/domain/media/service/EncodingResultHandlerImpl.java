package com.team08.backend.domain.media.service;

import com.team08.backend.domain.lecture.service.LectureDbService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.media.event.VideoRollbackEvent;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EncodingResultHandlerImpl implements EncodingResultHandler {

    private final LectureDbService lectureDbService;
    private final LectureRepository lectureRepository;
    private final LectureModificationRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handleSuccess(Long lectureId, String dbSavePath, String targetDirName, String description, Long instructorId) {
        if (description == null) {
            lectureDbService.updateLectureM3u8(lectureId, dbSavePath, targetDirName);
            return;
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        LectureModificationRequest modificationRequest = LectureModificationRequest.createPending(
                lecture,
                instructorId,
                description,
                dbSavePath
        );
        requestRepository.save(modificationRequest);

        eventPublisher.publishEvent(new VideoRollbackEvent(lectureId, targetDirName));
    }
}