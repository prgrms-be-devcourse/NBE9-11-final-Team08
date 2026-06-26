package com.team08.backend.domain.lecturemodificationrequest.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationApprovalRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.RequestStatus;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.media.event.VideoCleanUpEvent;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LectureModificationApprovalService {

    private final LectureModificationRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approveRequest(Long requestId, Long adminId) {
        LectureModificationRequest request = requestRepository.findByIdWithLecture(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_MODIFICATION_REQUEST_NOT_FOUND));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        request.approve(adminId);

        Lecture lecture = request.getLecture();
        String oldVideoUuid = lecture.getVideoUuid();

        lecture.updateM3u8Path(request.getAfterM3u8Path(), request.getAfterVideoUuid());

        if (oldVideoUuid != null && !oldVideoUuid.isBlank()) {
            eventPublisher.publishEvent(new VideoCleanUpEvent(lecture.getId(), oldVideoUuid));
        }
    }

    @Transactional
    public void rejectRequest(Long requestId, Long adminId, LectureModificationApprovalRequest approvalRequest) {
        if (approvalRequest.rejectedReason() == null || approvalRequest.rejectedReason().isBlank()) {
            throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        LectureModificationRequest request = requestRepository.findByIdWithLecture(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_MODIFICATION_REQUEST_NOT_FOUND));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        request.reject(approvalRequest.rejectedReason(), adminId);

        String rejectedVideoUuid = request.getAfterVideoUuid();
        if (rejectedVideoUuid != null && !rejectedVideoUuid.isBlank()) {
            eventPublisher.publishEvent(new VideoCleanUpEvent(request.getLecture().getId(), rejectedVideoUuid));
        }
    }
}