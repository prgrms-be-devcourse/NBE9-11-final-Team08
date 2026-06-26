package com.team08.backend.domain.feed.outbox;

import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.event.LearningEventRecorded;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedOutboxEventListenerTest {

    @Mock
    private FeedOutboxService feedOutboxService;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private LectureRepository lectureRepository;

    private FeedOutboxEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new FeedOutboxEventListener(
                feedOutboxService,
                studyRepository,
                studyMemberRepository,
                lectureRepository
        );
    }

    @Test
    void 강의_학습_이벤트를_feed_outbox로_전달한다() {
        LearningEventRecorded event = learningEvent(LearningEventType.LECTURE_COMPLETE);
        Study study = mock(Study.class);
        Lecture lecture = mock(Lecture.class);

        given(study.getId()).willReturn(10L);
        given(lecture.getTitle()).willReturn("스프링 이벤트 기초");
        given(studyRepository.findByCourseIdAndStatusIn(
                1L,
                List.of(StudyStatus.ACTIVE, StudyStatus.READONLY)
        )).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                10L,
                2L,
                StudyMemberStatus.ACTIVE
        )).willReturn(true);
        given(lectureRepository.findById(4L)).willReturn(Optional.of(lecture));

        listener.onLearningEventRecorded(event);

        verify(feedOutboxService).createLearningEventRecordedEvent(
                event,
                10L,
                "스프링 이벤트 기초"
        );
    }

    @Test
    void feed_대상이_아닌_학습_이벤트는_무시한다() {
        LearningEventRecorded event = learningEvent(LearningEventType.VIDEO_START);

        listener.onLearningEventRecorded(event);

        verify(studyRepository, never()).findByCourseIdAndStatusIn(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(feedOutboxService, never()).createLearningEventRecordedEvent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void active_member가_아니면_feed_outbox를_만들지_않는다() {
        LearningEventRecorded event = learningEvent(LearningEventType.LECTURE_ENTER);
        Study study = mock(Study.class);

        given(study.getId()).willReturn(10L);
        given(studyRepository.findByCourseIdAndStatusIn(
                1L,
                List.of(StudyStatus.ACTIVE, StudyStatus.READONLY)
        )).willReturn(Optional.of(study));
        given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(
                10L,
                2L,
                StudyMemberStatus.ACTIVE
        )).willReturn(false);

        listener.onLearningEventRecorded(event);

        verify(lectureRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(feedOutboxService, never()).createLearningEventRecordedEvent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private LearningEventRecorded learningEvent(LearningEventType type) {
        return new LearningEventRecorded(
                100L,
                2L,
                1L,
                3L,
                4L,
                type,
                null,
                LocalDateTime.of(2026, 6, 25, 14, 0)
        );
    }
}
