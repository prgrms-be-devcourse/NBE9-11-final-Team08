package com.team08.backend.domain.media.service;

import com.team08.backend.domain.lecture.service.LectureDbService;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.media.dto.EncodingContext;
import com.team08.backend.domain.media.entity.EncodingPurpose;
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
    public void handleSuccess(EncodingContext context) {

        eventPublisher.publishEvent(new VideoRollbackEvent(context.lectureId(), context.targetDirName()));

        if (context.purpose() == EncodingPurpose.CREATE) {
            lectureDbService.updateLectureM3u8(context.lectureId(), context.dbSavePath(), context.targetDirName());
            return;
        }

        if (context.purpose() == EncodingPurpose.MODIFY) {
            Lecture lecture = lectureRepository.findById(context.lectureId())
                    .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

            String requestDescription = (context.description() == null || context.description().isBlank())
                    ? "영상 수정 요청" : context.description();

            LectureModificationRequest modificationRequest = LectureModificationRequest.createPending(
                    lecture,
                    context.instructorId(),
                    requestDescription,
                    context.dbSavePath(),
                    context.targetDirName()
            );
            requestRepository.save(modificationRequest);
        }
    }
}