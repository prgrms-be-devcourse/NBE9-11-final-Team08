package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.domain.studyactivity.event.StudyActivityCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeedOutboxEventListener {

    private final FeedOutboxService feedOutboxService;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final LectureRepository lectureRepository;

    @EventListener
    public void onStudyActivityCreated(StudyActivityCreated event) {
        feedOutboxService.createStudyActivityCreatedEvent(event);
    }

    @EventListener
    public void onLearningEventRecorded(LearningEventRecorded event) {
        if (!isFeedTarget(event.eventType()) || !hasRequiredFeedSource(event)) {
            return;
        }

        Optional<Study> study = studyRepository.findByCourseIdAndStatusIn(
                event.courseId(),
                List.of(StudyStatus.ACTIVE, StudyStatus.READONLY)
        );
        if (study.isEmpty()) {
            return;
        }

        Long studyId = study.get().getId();
        if (!studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                studyId,
                event.userId(),
                StudyMemberStatus.ACTIVE
        )) {
            return;
        }

        lectureRepository.findById(event.lectureId())
                .ifPresent(lecture -> feedOutboxService.createLearningEventRecordedEvent(
                        event,
                        studyId,
                        lecture.getTitle()
                ));
    }

    private boolean isFeedTarget(LearningEventType eventType) {
        return eventType == LearningEventType.LECTURE_ENTER
                || eventType == LearningEventType.LECTURE_COMPLETE;
    }

    private boolean hasRequiredFeedSource(LearningEventRecorded event) {
        return event.learningEventId() != null
                && event.courseId() != null
                && event.lectureId() != null;
    }
}
