package com.team08.backend.domain.aifeedback.service;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedbackStatus;
import com.team08.backend.domain.aifeedback.generator.AiFeedbackGenerator;
import com.team08.backend.domain.aifeedback.repository.AiFeedbackRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiFeedbackGenerator aiFeedbackGenerator;

    public AiFeedbackResponse generate(
            Long studyId,
            Long activityId,
            Long userId
    ) {
        Study study = findStudy(studyId);
        validateActiveStudy(study);
        validateActiveMember(studyId, userId);

        StudyActivity activity = findActivity(studyId, activityId);
        validateAuthor(activity, userId);

        String contentSnapshot = activity.getContent();
        Optional<AiFeedback> existingFeedback =
                aiFeedbackRepository.findByStudyActivityIdForUpdate(activityId);

        if (existingFeedback.isPresent()) {
            AiFeedback feedback = existingFeedback.get();
            if (feedback.getStatus() == AiFeedbackStatus.PROCESSING) {
                throw new CustomException(ErrorCode.AI_FEEDBACK_GENERATION_IN_PROGRESS);
            }
            if (feedback.getStatus() == AiFeedbackStatus.COMPLETED
                    && feedback.hasSameSnapshot(contentSnapshot)) {
                return response(feedback);
            }
            feedback.restartProcessing(
                    contentSnapshot,
                    aiFeedbackGenerator.modelName(),
                    aiFeedbackGenerator.promptVersion()
            );
            aiFeedbackRepository.save(feedback);
            return generateAndSave(feedback, studyId, activityId, contentSnapshot);
        }

        AiFeedback feedback = AiFeedback.startProcessing(
                userId,
                studyId,
                activityId,
                contentSnapshot,
                aiFeedbackGenerator.modelName(),
                aiFeedbackGenerator.promptVersion()
        );
        saveNewFeedback(feedback);

        return generateAndSave(feedback, studyId, activityId, contentSnapshot);
    }

    public AiFeedbackResponse get(
            Long studyId,
            Long activityId,
            Long userId
    ) {
        validateVisibleStudy(studyId);
        validateActiveMember(studyId, userId);
        findActivity(studyId, activityId);

        AiFeedback feedback = aiFeedbackRepository.findByStudyActivityId(activityId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.AI_FEEDBACK_NOT_FOUND)
                );
        return response(feedback);
    }

    private AiFeedbackResponse generateAndSave(
            AiFeedback feedback,
            Long studyId,
            Long activityId,
            String contentSnapshot
    ) {
        try {
            StructuredFeedback result =
                    aiFeedbackGenerator.generateKorean(contentSnapshot);
            feedback.complete(AiFeedbackJson.write(result));

            String currentContent = findActivity(studyId, activityId).getContent();
            if (!contentSnapshot.equals(currentContent)) {
                feedback.markStale();
            }

            aiFeedbackRepository.save(feedback);
            return AiFeedbackResponse.from(feedback, result);
        } catch (CustomException e) {
            feedback.fail();
            aiFeedbackRepository.save(feedback);
            throw e;
        } catch (RuntimeException e) {
            feedback.fail();
            aiFeedbackRepository.save(feedback);
            throw new CustomException(ErrorCode.AI_FEEDBACK_GENERATION_FAILED);
        }
    }

    private void saveNewFeedback(AiFeedback feedback) {
        try {
            aiFeedbackRepository.saveAndFlush(feedback);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.AI_FEEDBACK_GENERATION_IN_PROGRESS);
        }
    }

    private AiFeedbackResponse response(AiFeedback feedback) {
        return AiFeedbackResponse.from(
                feedback,
                AiFeedbackJson.read(feedback.getFeedback())
        );
    }

    private Study findStudy(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private void validateVisibleStudy(Long studyId) {
        studyRepository.findByIdAndStatusNot(studyId, StudyStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));
    }

    private void validateActiveStudy(Study study) {
        if (study.getStatus() != StudyStatus.ACTIVE) {
            throw new CustomException(ErrorCode.STUDY_NOT_ACTIVE);
        }
    }

    private void validateActiveMember(Long studyId, Long userId) {
        boolean isActiveMember =
                studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                        studyId,
                        userId,
                        StudyMemberStatus.ACTIVE
                );

        if (!isActiveMember) {
            throw new CustomException(ErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private StudyActivity findActivity(Long studyId, Long activityId) {
        return studyActivityRepository
                .findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND)
                );
    }

    private void validateAuthor(StudyActivity activity, Long userId) {
        if (!activity.getAuthorId().equals(userId)) {
            throw new CustomException(ErrorCode.AI_FEEDBACK_REQUEST_DENIED);
        }
    }
}
