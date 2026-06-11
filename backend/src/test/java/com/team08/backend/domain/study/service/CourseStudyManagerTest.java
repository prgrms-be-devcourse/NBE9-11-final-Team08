package com.team08.backend.domain.study.service;

import com.team08.backend.domain.study.command.CourseStudyCreateCommand;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.exception.DuplicateStudyException;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberRole;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseStudyManagerTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private CourseStudyManagerImpl courseStudyManager;

    @Test
    void 강좌용_스터디를_생성하면_스터디와_소유자_멤버가_함께_생성된다() {
        // given
        Long ownerId = 1L;
        Long courseId = 10L;

        CourseStudyCreateCommand command = new CourseStudyCreateCommand(
                ownerId,
                courseId,
                "스프링 강좌 스터디",
                "강좌 기반 스터디"
        );

        given(studyRepository.existsByCourseId(courseId))
                .willReturn(false);

        ArgumentCaptor<Study> studyCaptor =
                ArgumentCaptor.forClass(Study.class);

        ArgumentCaptor<StudyMember> memberCaptor =
                ArgumentCaptor.forClass(StudyMember.class);

        // when
        courseStudyManager.createForCourse(command);

        // then
        verify(studyRepository).save(studyCaptor.capture());
        verify(studyMemberRepository).save(memberCaptor.capture());

        Study savedStudy = studyCaptor.getValue();
        StudyMember savedMember = memberCaptor.getValue();

        assertThat(savedStudy.getCourse().getId()).isEqualTo(courseId);
        assertThat(savedStudy.getOwner().getId()).isEqualTo(ownerId);
        assertThat(savedStudy.getStatus()).isEqualTo(StudyStatus.DRAFT);

        assertThat(savedMember.getUser().getId()).isEqualTo(ownerId);
        assertThat(savedMember.getRole()).isEqualTo(StudyMemberRole.OWNER);
        assertThat(savedMember.getStatus()).isEqualTo(StudyMemberStatus.ACTIVE);
    }

    @Test
    void 동일한_강좌의_스터디가_존재하면_예외가_발생한다() {
        // given
        Long courseId = 10L;

        CourseStudyCreateCommand command = new CourseStudyCreateCommand(
                1L,
                courseId,
                "스프링 강좌 스터디",
                "강좌 기반 스터디"
        );

        given(studyRepository.existsByCourseId(courseId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                courseStudyManager.createForCourse(command)
        ).isInstanceOf(DuplicateStudyException.class);
    }
}
