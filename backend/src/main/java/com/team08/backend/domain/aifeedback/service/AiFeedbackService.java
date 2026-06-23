package com.team08.backend.domain.aifeedback.service;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedbackStatus;
import com.team08.backend.domain.aifeedback.generator.AiFeedbackGenerator;
import com.team08.backend.domain.aifeedback.repository.AiFeedbackRepository;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final StudyActivityRepository studyActivityRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiFeedbackGenerator aiFeedbackGenerator;
    private final StudyAccessAuthorizer studyAccessAuthorizer;

    public AiFeedbackResponse generate(
            Long studyId,
            Long activityId,
            Long userId
    ) {
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.WRITE_STUDY_CONTENT);

        StudyActivity activity = findActivity(studyId, activityId);
        activity.validateAuthor(userId);

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
        studyAccessAuthorizer.authorizeByStudyId(studyId, userId, StudyAction.VIEW_STUDY_CONTENT);

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

    private StudyActivity findActivity(Long studyId, Long activityId) {
        return studyActivityRepository
                .findByIdAndStudyIdAndDeletedAtIsNull(activityId, studyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND)
                );
    }
}
